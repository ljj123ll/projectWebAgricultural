package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicDataCacheService {

    public static final String HOME_KEY = "cache:user:home";
    public static final String PRODUCT_DETAIL_KEY_PREFIX = "cache:user:product:detail:";
    public static final String PRODUCT_SEARCH_KEY_PREFIX = "cache:user:product:search:";
    public static final String PRODUCT_HOT_LIST_KEY_PREFIX = "cache:user:product:hot:";
    public static final String PRODUCT_ORIGINS_KEY = "cache:user:product:origins";
    public static final String PRODUCT_CATEGORIES_KEY = "cache:user:product:categories";
    public static final String PRODUCT_HOT_ZSET_KEY = "cache:user:product:hot:zset";
    public static final String MERCHANT_SHOP_KEY_PREFIX = "cache:user:merchant:shop:";
    public static final String MERCHANT_PRODUCTS_KEY_PREFIX = "cache:user:merchant:products:";
    public static final String NEWS_LIST_KEY_PREFIX = "cache:user:news:list:";
    public static final String NEWS_CATEGORY_KEY = "cache:user:news:categories";
    public static final String UNSALABLE_LIST_KEY_PREFIX = "cache:user:unsalable:list:";
    public static final String UNSALABLE_SUMMARY_KEY_PREFIX = "cache:user:unsalable:summary:";

    private static final Duration HOT_RANK_TTL = Duration.ofHours(6);

    private final RedisCacheService redisCacheService;
    private final StringRedisTemplate redisTemplate;
    private final ProductInfoMapper productInfoMapper;

    public void evictHome() {
        redisCacheService.delete(HOME_KEY);
    }

    public void evictProductDetail(Long productId) {
        if (productId != null) {
            redisCacheService.delete(PRODUCT_DETAIL_KEY_PREFIX + productId);
        }
    }

    public void evictProductSearch() {
        redisCacheService.deleteByPrefix(PRODUCT_SEARCH_KEY_PREFIX);
    }

    public void evictHotProducts() {
        redisCacheService.deleteByPrefix(PRODUCT_HOT_LIST_KEY_PREFIX);
    }

    public void evictMerchantProducts(Long merchantId) {
        if (merchantId == null) {
            redisCacheService.deleteByPrefix(MERCHANT_PRODUCTS_KEY_PREFIX);
            redisCacheService.deleteByPrefix(MERCHANT_SHOP_KEY_PREFIX);
            return;
        }
        redisCacheService.deleteByPrefix(MERCHANT_PRODUCTS_KEY_PREFIX + merchantId + ":");
        redisCacheService.delete(MERCHANT_SHOP_KEY_PREFIX + merchantId);
    }

    public void evictNews() {
        redisCacheService.deleteByPrefix(NEWS_LIST_KEY_PREFIX);
        redisCacheService.delete(NEWS_CATEGORY_KEY);
        evictHome();
    }

    public void evictUnsalable() {
        redisCacheService.deleteByPrefix(UNSALABLE_LIST_KEY_PREFIX);
        redisCacheService.deleteByPrefix(UNSALABLE_SUMMARY_KEY_PREFIX);
        evictHome();
    }

    public void evictProductCatalog(Long productId, Long merchantId) {
        evictHome();
        evictProductDetail(productId);
        evictProductSearch();
        evictHotProducts();
        evictUnsalable();
        evictMerchantProducts(merchantId);
        redisCacheService.delete(PRODUCT_ORIGINS_KEY);
    }

    public List<Long> getHotProductIds(int limit) {
        if (limit < 1) {
            return List.of();
        }
        initializeHotRankIfNecessary();
        Set<String> ids = redisTemplate.opsForZSet().reverseRange(PRODUCT_HOT_ZSET_KEY, 0, limit - 1L);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids) {
            if (StringUtils.hasText(id) && id.matches("\\d+")) {
                result.add(Long.parseLong(id));
            }
        }
        return result;
    }

    public void refreshHotProduct(ProductInfo product) {
        if (product == null || product.getId() == null) {
            return;
        }
        boolean online = (product.getDeleteFlag() == null || product.getDeleteFlag() == 0)
                && product.getStatus() != null
                && product.getStatus() == 1;
        if (!online) {
            redisTemplate.opsForZSet().remove(PRODUCT_HOT_ZSET_KEY, String.valueOf(product.getId()));
            return;
        }
        double score = product.getSalesVolume() == null ? 0D : product.getSalesVolume().doubleValue();
        redisTemplate.opsForZSet().add(PRODUCT_HOT_ZSET_KEY, String.valueOf(product.getId()), score);
        redisTemplate.expire(PRODUCT_HOT_ZSET_KEY, HOT_RANK_TTL);
    }

    public void removeHotProduct(Long productId) {
        if (productId != null) {
            redisTemplate.opsForZSet().remove(PRODUCT_HOT_ZSET_KEY, String.valueOf(productId));
        }
    }

    private void initializeHotRankIfNecessary() {
        Long size = redisTemplate.opsForZSet().zCard(PRODUCT_HOT_ZSET_KEY);
        if (size != null && size > 0) {
            return;
        }
        List<ProductInfo> products = productInfoMapper.selectList(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1)
                .eq(ProductInfo::getDeleteFlag, 0)
                .orderByDesc(ProductInfo::getSalesVolume)
                .orderByDesc(ProductInfo::getCreateTime));
        products.stream()
                .sorted(Comparator.comparing(ProductInfo::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(this::refreshHotProduct);
    }
}
