package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ProductTrace;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ProductTraceMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.vo.user.ProductDetailVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProductService {

    private final ProductInfoMapper productInfoMapper;
    private final ProductTraceMapper productTraceMapper;
    private final ShopInfoMapper shopInfoMapper;

    /**
     * 商品搜索：keyword、sortBy(sales/price_asc/price_desc/score)、categoryId、originPlace、分页
     */
    public PageResult<ProductSimpleVO> search(String keyword, String sortBy, Long categoryId, String originPlace,
                                             Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<ProductInfo> q = new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(ProductInfo::getProductName, keyword).or().like(ProductInfo::getOriginPlace, keyword));
        }
        if (categoryId != null) q.eq(ProductInfo::getCategoryId, categoryId);
        if (originPlace != null && !originPlace.isBlank()) q.eq(ProductInfo::getOriginPlace, originPlace);
        if ("sales".equals(sortBy)) q.orderByDesc(ProductInfo::getSalesVolume);
        else if ("price_asc".equals(sortBy)) q.orderByAsc(ProductInfo::getPrice);
        else if ("price_desc".equals(sortBy)) q.orderByDesc(ProductInfo::getPrice);
        else if ("score".equals(sortBy)) q.orderByDesc(ProductInfo::getScore);
        else q.orderByDesc(ProductInfo::getSalesVolume);
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<ProductSimpleVO> list = page.getRecords().stream().map(this::toSimpleVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), list);
    }

    public ProductDetailVO getDetail(Long id) {
        ProductInfo p = productInfoMapper.selectById(id);
        if (p == null || p.getStatus() != 1) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(p.getId());
        vo.setProductName(p.getProductName());
        vo.setPrice(p.getPrice());
        vo.setCategoryId(p.getCategoryId());
        vo.setStock(p.getStock());
        vo.setProductImg(p.getProductImg());
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

    /**
     * 热销榜单（可配合 Redis ZSet 缓存）
     */
    public List<ProductSimpleVO> hotList(Integer limit) {
        if (limit == null || limit < 1) limit = 10;
        List<ProductInfo> list = productInfoMapper.selectList(
                new LambdaQueryWrapper<ProductInfo>().eq(ProductInfo::getStatus, 1)
                        .orderByDesc(ProductInfo::getSalesVolume).last("LIMIT " + limit));
        return list.stream().map(this::toSimpleVO).collect(Collectors.toList());
    }

    private ProductSimpleVO toSimpleVO(ProductInfo p) {
        ProductSimpleVO v = new ProductSimpleVO();
        v.setId(p.getId());
        v.setProductName(p.getProductName());
        v.setPrice(p.getPrice());
        v.setSalesVolume(p.getSalesVolume() != null ? p.getSalesVolume() : 0);
        v.setStock(p.getStock() != null ? p.getStock() : 0);
        v.setProductImg(p.getProductImg());
        v.setScore(p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO);
        return v;
    }
}
