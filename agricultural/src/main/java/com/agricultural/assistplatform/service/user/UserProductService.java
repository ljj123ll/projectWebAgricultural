package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.ProductCategory;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ProductTrace;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ProductTraceMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.mapper.ProductCategoryMapper;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.RedisCacheService;
import com.agricultural.assistplatform.vo.user.ProductDetailVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProductService {

    private final ProductInfoMapper productInfoMapper;
    private final ProductTraceMapper productTraceMapper;
    private final ShopInfoMapper shopInfoMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final RedisCacheService redisCacheService;
    private final PublicDataCacheService publicDataCacheService;

    private static final Duration PRODUCT_SEARCH_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration PRODUCT_DETAIL_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration PRODUCT_HOT_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration PRODUCT_META_CACHE_TTL = Duration.ofHours(1);

    /**
     * 商品搜索：keyword（商品名/产地/店铺名/商家名）、sortBy、categoryId、originPlace、isUnsalable、分页
     */
    public PageResult<ProductSimpleVO> search(String keyword, String sortBy, Long categoryId, String originPlace, Integer isUnsalable,
                                             Integer pageNum, Integer pageSize) {
        String cacheKey = PublicDataCacheService.PRODUCT_SEARCH_KEY_PREFIX
                + normalize(keyword) + ":" + normalize(sortBy) + ":" + normalize(categoryId)
                + ":" + normalize(originPlace) + ":" + normalize(isUnsalable) + ":" + normalize(pageNum) + ":" + normalize(pageSize);
        return redisCacheService.getOrLoad(cacheKey, PRODUCT_SEARCH_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> doSearch(keyword, sortBy, categoryId, originPlace, isUnsalable, pageNum, pageSize));
    }

    public ProductDetailVO getDetail(Long id) {
        return redisCacheService.getOrLoad(
                PublicDataCacheService.PRODUCT_DETAIL_KEY_PREFIX + id,
                PRODUCT_DETAIL_CACHE_TTL,
                ProductDetailVO.class,
                () -> loadDetail(id)
        );
    }

    /**
     * 热销榜单（优先走 Redis 热门商品排序）
     */
    public List<ProductSimpleVO> hotList(Integer limit) {
        int safeLimit = (limit == null || limit < 1) ? 10 : limit;
        String cacheKey = PublicDataCacheService.PRODUCT_HOT_LIST_KEY_PREFIX + safeLimit;
        return redisCacheService.getOrLoad(cacheKey, PRODUCT_HOT_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> loadHotList(safeLimit));
    }

    public List<ProductCategory> categories() {
        return redisCacheService.getOrLoad(PublicDataCacheService.PRODUCT_CATEGORIES_KEY, PRODUCT_META_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getDeleteFlag, 0)
                        .eq(ProductCategory::getCategoryLevel, 1)
                        .orderByAsc(ProductCategory::getId)));
    }

    public List<String> origins() {
        return redisCacheService.getOrLoad(PublicDataCacheService.PRODUCT_ORIGINS_KEY, PRODUCT_META_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> productInfoMapper.selectList(new LambdaQueryWrapper<ProductInfo>().isNotNull(ProductInfo::getOriginPlace))
                        .stream().map(ProductInfo::getOriginPlace).distinct().collect(Collectors.toList()));
    }

    private PageResult<ProductSimpleVO> doSearch(String keyword, String sortBy, Long categoryId, String originPlace, Integer isUnsalable,
                                                 Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<ProductInfo> q = new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            Set<Long> matchedShopMerchantIds = shopInfoMapper.selectList(
                            new LambdaQueryWrapper<ShopInfo>().like(ShopInfo::getShopName, kw))
                    .stream()
                    .map(ShopInfo::getMerchantId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            Set<Long> matchedMerchantNameIds = merchantInfoMapper.selectList(
                            new LambdaQueryWrapper<MerchantInfo>().like(MerchantInfo::getMerchantName, kw))
                    .stream()
                    .map(MerchantInfo::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            Set<Long> matchedMerchantIds = new HashSet<>();
            matchedMerchantIds.addAll(matchedShopMerchantIds);
            matchedMerchantIds.addAll(matchedMerchantNameIds);
            if (matchedMerchantIds.isEmpty()) {
                q.and(w -> w.like(ProductInfo::getProductName, kw)
                        .or().like(ProductInfo::getOriginPlace, kw));
            } else {
                q.and(w -> w.like(ProductInfo::getProductName, kw)
                        .or().like(ProductInfo::getOriginPlace, kw)
                        .or().in(ProductInfo::getMerchantId, matchedMerchantIds));
            }
        }
        if (categoryId != null) q.eq(ProductInfo::getCategoryId, categoryId);
        if (originPlace != null && !originPlace.isBlank()) q.likeRight(ProductInfo::getOriginPlace, originPlace);
        if (isUnsalable != null) q.eq(ProductInfo::getIsUnsalable, isUnsalable);
        if ("sales".equals(sortBy)) q.orderByDesc(ProductInfo::getSalesVolume);
        else if ("price_asc".equals(sortBy)) q.orderByAsc(ProductInfo::getPrice);
        else if ("price_desc".equals(sortBy)) q.orderByDesc(ProductInfo::getPrice);
        else if ("score".equals(sortBy)) q.orderByDesc(ProductInfo::getScore);
        else if ("stock".equals(sortBy)) q.orderByDesc(ProductInfo::getStock);
        else q.orderByDesc(ProductInfo::getSalesVolume);
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        Map<Long, String> categoryNameMap = loadCategoryNameMap(page.getRecords());
        Map<Long, String> merchantNameMap = loadMerchantNameMap(page.getRecords());
        List<ProductSimpleVO> list = page.getRecords().stream()
                .map(product -> toSimpleVO(product, categoryNameMap, merchantNameMap))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), list);
    }

    private ProductDetailVO loadDetail(Long id) {
        ProductInfo p = productInfoMapper.selectById(id);
        if (p == null || p.getStatus() != 1) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(p.getId());
        vo.setProductName(p.getProductName());
        vo.setPrice(p.getPrice());
        vo.setCategoryId(p.getCategoryId());
        if (p.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(p.getCategoryId());
            vo.setCategoryName(category != null ? category.getCategoryName() : null);
        }
        vo.setStock(p.getStock());
        vo.setProductImg(p.getProductImg());
        vo.setProductDetailImg(p.getProductDetailImg());
        vo.setProductDesc(p.getProductDesc());
        vo.setOriginPlace(p.getOriginPlace());
        vo.setSalesVolume(p.getSalesVolume() != null ? p.getSalesVolume() : 0);
        vo.setScore(p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO);
        vo.setMerchantId(p.getMerchantId());
        ShopInfo shop = shopInfoMapper.selectOne(
                new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, p.getMerchantId()));
        vo.setShopName(shop != null ? shop.getShopName() : null);
        ProductTrace trace = productTraceMapper.selectOne(
                new LambdaQueryWrapper<ProductTrace>().eq(ProductTrace::getProductId, id));
        if (trace != null) {
            vo.setPlantingCycle(trace.getPlantingCycle());
            vo.setOriginPlaceDetail(trace.getOriginPlaceDetail());
            vo.setFertilizerType(trace.getFertilizerType());
            vo.setStorageMethod(trace.getStorageMethod());
            vo.setTransportMethod(trace.getTransportMethod());
            vo.setQrCodeUrl(trace.getQrCodeUrl());
        }
        return vo;
    }

    private List<ProductSimpleVO> loadHotList(int limit) {
        List<Long> hotIds = publicDataCacheService.getHotProductIds(limit);
        if (hotIds.isEmpty()) {
            List<ProductInfo> list = productInfoMapper.selectList(
                    new LambdaQueryWrapper<ProductInfo>().eq(ProductInfo::getStatus, 1)
                            .eq(ProductInfo::getDeleteFlag, 0)
                            .orderByDesc(ProductInfo::getSalesVolume).last("LIMIT " + limit));
            Map<Long, String> categoryNameMap = loadCategoryNameMap(list);
            Map<Long, String> merchantNameMap = loadMerchantNameMap(list);
            return list.stream()
                    .map(product -> toSimpleVO(product, categoryNameMap, merchantNameMap))
                    .collect(Collectors.toList());
        }

        Map<Long, ProductInfo> productMap = new HashMap<>();
        List<ProductInfo> products = productInfoMapper.selectBatchIds(hotIds);
        products.forEach(product -> productMap.put(product.getId(), product));
        Map<Long, String> categoryNameMap = loadCategoryNameMap(products);
        Map<Long, String> merchantNameMap = loadMerchantNameMap(products);
        return hotIds.stream()
                .map(productMap::get)
                .filter(product -> product != null && product.getStatus() != null && product.getStatus() == 1)
                .sorted(Comparator.comparing(ProductInfo::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(product -> toSimpleVO(product, categoryNameMap, merchantNameMap))
                .collect(Collectors.toList());
    }

    private ProductSimpleVO toSimpleVO(ProductInfo p, Map<Long, String> categoryNameMap, Map<Long, String> merchantNameMap) {
        ProductSimpleVO v = new ProductSimpleVO();
        v.setId(p.getId());
        v.setProductName(p.getProductName());
        v.setPrice(p.getPrice());
        v.setSalesVolume(p.getSalesVolume() != null ? p.getSalesVolume() : 0);
        v.setStock(p.getStock() != null ? p.getStock() : 0);
        v.setProductImg(p.getProductImg());
        v.setScore(p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO);
        v.setOriginPlace(p.getOriginPlace());
        v.setCategoryId(p.getCategoryId());
        v.setCategoryName(categoryNameMap.get(p.getCategoryId()));
        v.setMerchantName(merchantNameMap.get(p.getMerchantId()));
        return v;
    }

    private Map<Long, String> loadCategoryNameMap(List<ProductInfo> products) {
        Set<Long> categoryIds = products.stream()
                .map(ProductInfo::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .in(ProductCategory::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, String> loadMerchantNameMap(List<ProductInfo> products) {
        Set<Long> merchantIds = products.stream()
                .map(ProductInfo::getMerchantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (merchantIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> merchantNameMap = new HashMap<>();
        shopInfoMapper.selectList(new LambdaQueryWrapper<ShopInfo>().in(ShopInfo::getMerchantId, merchantIds))
                .forEach(shop -> {
                    if (shop.getMerchantId() != null && shop.getShopName() != null) {
                        merchantNameMap.put(shop.getMerchantId(), shop.getShopName());
                    }
                });

        Set<Long> missingMerchantIds = merchantIds.stream()
                .filter(id -> !merchantNameMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!missingMerchantIds.isEmpty()) {
            merchantInfoMapper.selectList(new LambdaQueryWrapper<MerchantInfo>().in(MerchantInfo::getId, missingMerchantIds))
                    .forEach(merchant -> {
                        if (merchant.getId() != null && merchant.getMerchantName() != null) {
                            merchantNameMap.put(merchant.getId(), merchant.getMerchantName());
                        }
                    });
        }
        return merchantNameMap;
    }

    private String normalize(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "blank" : text.replace(":", "_");
    }
}
