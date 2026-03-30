package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.Comment;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.mapper.CommentMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.RedisCacheService;
import com.agricultural.assistplatform.vo.user.MerchantCommentVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.agricultural.assistplatform.vo.user.UserMerchantShopVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 用户端商家页服务。
 * 负责店铺主页、商家商品列表、商家评论聚合，以及商家评分的计算。
 */
public class UserMerchantService {

    private final ShopInfoMapper shopInfoMapper;
    private final ProductInfoMapper productInfoMapper;
    private final CommentMapper commentMapper;
    private final UserInfoMapper userInfoMapper;
    private final RedisCacheService redisCacheService;

    private static final Duration MERCHANT_CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 查询商家主页信息。
     * 商家评分直接基于全部已通过评论计算，避免按商品均分带来的失真。
     */
    public UserMerchantShopVO getShop(Long merchantId) {
        ShopInfo shopInfo = shopInfoMapper.selectOne(
                new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId)
        );
        if (shopInfo == null) {
            return null;
        }

        List<ProductInfo> products = productInfoMapper.selectList(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getMerchantId, merchantId)
                .in(ProductInfo::getStatus, 1, 2));

        Set<Long> productIds = products.stream()
                .map(ProductInfo::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Comment> comments = productIds.isEmpty()
                ? Collections.emptyList()
                : commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .in(Comment::getProductId, productIds)
                .eq(Comment::getAuditStatus, 1));

        UserMerchantShopVO vo = new UserMerchantShopVO();
        vo.setId(shopInfo.getId());
        vo.setMerchantId(shopInfo.getMerchantId());
        vo.setShopName(shopInfo.getShopName());
        vo.setShopIntro(shopInfo.getShopIntro());
        vo.setQualificationImg(shopInfo.getQualificationImg());
        vo.setShopType(shopInfo.getShopType());
        vo.setCategories(shopInfo.getCategories());
        vo.setShopAddress(shopInfo.getShopAddress());
        vo.setProductCount(products.stream().filter(product -> Integer.valueOf(1).equals(product.getStatus())).count());
        vo.setCommentCount((long) comments.size());
        vo.setReviewCount(comments.size());
        vo.setTotalSalesVolume(products.stream()
                .map(ProductInfo::getSalesVolume)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());

        List<Integer> commentScores = comments.stream()
                .map(Comment::getScore)
                .filter(Objects::nonNull)
                .toList();
        if (!commentScores.isEmpty()) {
            BigDecimal scoreSum = commentScores.stream()
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal averageScore = scoreSum
                    .divide(BigDecimal.valueOf(commentScores.size()), 1, RoundingMode.HALF_UP);
            vo.setAverageScore(averageScore);
        }

        return vo;
    }

    /**
     * 商家商品列表，带 Redis 分页缓存。
     */
    public PageResult<ProductSimpleVO> products(Long merchantId, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        String cacheKey = PublicDataCacheService.MERCHANT_PRODUCTS_KEY_PREFIX + merchantId + ":" + safePageNum + ":" + safePageSize;
        return redisCacheService.getOrLoad(cacheKey, MERCHANT_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> {
                    Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(safePageNum, safePageSize),
                            new LambdaQueryWrapper<ProductInfo>()
                                    .eq(ProductInfo::getMerchantId, merchantId)
                                    .eq(ProductInfo::getStatus, 1)
                                    .orderByDesc(ProductInfo::getSalesVolume)
                                    .orderByDesc(ProductInfo::getCreateTime));

                    List<ProductSimpleVO> list = page.getRecords().stream().map(product -> {
                        ProductSimpleVO vo = new ProductSimpleVO();
                        vo.setId(product.getId());
                        vo.setProductName(product.getProductName());
                        vo.setCategoryId(product.getCategoryId());
                        vo.setPrice(product.getPrice());
                        vo.setStock(product.getStock());
                        vo.setProductImg(product.getProductImg());
                        vo.setOriginPlace(product.getOriginPlace());
                        vo.setSalesVolume(product.getSalesVolume() != null ? product.getSalesVolume() : 0);
                        vo.setScore(product.getScore() != null ? product.getScore() : BigDecimal.ZERO);
                        return vo;
                    }).collect(Collectors.toList());

                    return PageResult.of(page.getTotal(), list);
                });
    }

    /**
     * 商家评论聚合页，汇总商家全部商品的已审核评论并补齐用户、商品信息。
     */
    public PageResult<MerchantCommentVO> comments(Long merchantId, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<ProductInfo> merchantProducts = productInfoMapper.selectList(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getMerchantId, merchantId)
                .select(ProductInfo::getId, ProductInfo::getProductName, ProductInfo::getProductImg));

        Set<Long> productIds = merchantProducts.stream()
                .map(ProductInfo::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return PageResult.of(0L, Collections.emptyList());
        }

        Page<Comment> page = commentMapper.selectPage(
                new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Comment>()
                        .in(Comment::getProductId, productIds)
                        .eq(Comment::getAuditStatus, 1)
                        .orderByDesc(Comment::getCreateTime)
        );

        List<Comment> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.of(page.getTotal(), Collections.emptyList());
        }

        Map<Long, ProductInfo> productMap = merchantProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(ProductInfo::getId, product -> product, (left, right) -> left));

        Set<Long> userIds = records.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserInfo> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getId, userIds))
                .stream()
                .collect(Collectors.toMap(UserInfo::getId, user -> user, (left, right) -> left));

        List<MerchantCommentVO> list = records.stream().map(comment -> {
            MerchantCommentVO vo = new MerchantCommentVO();
            vo.setId(comment.getId());
            vo.setOrderNo(comment.getOrderNo());
            vo.setProductId(comment.getProductId());
            vo.setUserId(comment.getUserId());
            vo.setScore(comment.getScore());
            vo.setContent(comment.getContent());
            vo.setImgUrls(comment.getImgUrls());
            vo.setMediaUrls(comment.getMediaUrls());
            vo.setCreateTime(comment.getCreateTime());

            ProductInfo product = productMap.get(comment.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setProductImg(product.getProductImg());
            }

            UserInfo user = userMap.get(comment.getUserId());
            String nickname = user != null && user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getNickname()
                    : "用户" + comment.getUserId();
            vo.setNickname(nickname);
            vo.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), list);
    }
}
