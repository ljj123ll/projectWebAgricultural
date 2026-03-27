package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.RedisCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserMerchantService {

    private final ShopInfoMapper shopInfoMapper;
    private final ProductInfoMapper productInfoMapper;
    private final RedisCacheService redisCacheService;

    private static final Duration MERCHANT_CACHE_TTL = Duration.ofMinutes(10);

    public ShopInfo getShop(Long merchantId) {
        return redisCacheService.getOrLoad(
                PublicDataCacheService.MERCHANT_SHOP_KEY_PREFIX + merchantId,
                MERCHANT_CACHE_TTL,
                ShopInfo.class,
                () -> shopInfoMapper.selectOne(new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId))
        );
    }

    public PageResult<ProductInfo> products(Long merchantId, Integer pageNum, Integer pageSize) {
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
                    return PageResult.of(page.getTotal(), page.getRecords());
                });
    }
}
