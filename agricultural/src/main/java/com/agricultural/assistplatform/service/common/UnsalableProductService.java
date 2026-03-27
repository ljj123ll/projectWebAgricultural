package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ProductCategory;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ProductCategoryMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.vo.common.UnsalableProductVO;
import com.agricultural.assistplatform.vo.common.UnsalableSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnsalableProductService {

    private final ProductInfoMapper productInfoMapper;
    private final ShopInfoMapper shopInfoMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final RedisCacheService redisCacheService;

    private static final Duration UNSALABLE_CACHE_TTL = Duration.ofMinutes(5);

    public PageResult<UnsalableProductVO> list(String keyword, String sortBy, String sourceType, Integer pageNum, Integer pageSize) {
        String cacheKey = PublicDataCacheService.UNSALABLE_LIST_KEY_PREFIX
                + normalize(keyword) + ":" + normalize(sortBy) + ":" + normalize(sourceType)
                + ":" + normalize(pageNum) + ":" + normalize(pageSize);
        return redisCacheService.getOrLoad(cacheKey, UNSALABLE_CACHE_TTL,
                new TypeReference<>() {
                },
                () -> loadList(keyword, sortBy, sourceType, pageNum, pageSize));
    }

    public UnsalableSummaryVO summary(String keyword) {
        String cacheKey = PublicDataCacheService.UNSALABLE_SUMMARY_KEY_PREFIX + normalize(keyword);
        return redisCacheService.getOrLoad(cacheKey, UNSALABLE_CACHE_TTL, UnsalableSummaryVO.class, () -> loadSummary(keyword));
    }

    private List<UnsalableProductVO> buildRecords(String keyword, String sourceType) {
        List<ProductInfo> products = productInfoMapper.selectList(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1)
                .eq(ProductInfo::getDeleteFlag, 0)
                .orderByDesc(ProductInfo::getUpdateTime)
                .orderByDesc(ProductInfo::getCreateTime));

        Set<Long> merchantIds = products.stream()
                .map(ProductInfo::getMerchantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = products.stream()
                .map(ProductInfo::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, String> merchantNameMap = loadMerchantNameMap(merchantIds);
        Map<Long, String> categoryNameMap = loadCategoryNameMap(categoryIds);

        List<UnsalableProductVO> result = new ArrayList<>();
        for (ProductInfo product : products) {
            AlgorithmDecision decision = evaluate(product);
            boolean manualIncluded = product.getIsUnsalable() != null && product.getIsUnsalable() == 1;
            boolean algorithmIncluded = decision.included();
            if (!manualIncluded && !algorithmIncluded) {
                continue;
            }

            UnsalableProductVO vo = toVO(product, merchantNameMap, categoryNameMap);
            vo.setManualIncluded(manualIncluded);
            vo.setAlgorithmIncluded(algorithmIncluded);
            vo.setAgeDays(decision.ageDays());
            vo.setUnsalableScore(decision.score());
            vo.setUnsalableReason(buildReason(manualIncluded, algorithmIncluded, decision, product));
            if (manualIncluded && algorithmIncluded) vo.setInclusionSource("manual_algorithm");
            else if (manualIncluded) vo.setInclusionSource("manual");
            else vo.setInclusionSource("algorithm");

            if (!matchesKeyword(vo, keyword) || !matchesSource(vo, sourceType)) {
                continue;
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, String> loadMerchantNameMap(Set<Long> merchantIds) {
        Map<Long, String> merchantNameMap = new HashMap<>();
        if (merchantIds.isEmpty()) return merchantNameMap;

        shopInfoMapper.selectList(new LambdaQueryWrapper<ShopInfo>().in(ShopInfo::getMerchantId, merchantIds))
                .forEach(shop -> {
                    if (shop.getMerchantId() != null && shop.getShopName() != null && !shop.getShopName().isBlank()) {
                        merchantNameMap.put(shop.getMerchantId(), shop.getShopName());
                    }
                });

        merchantInfoMapper.selectList(new LambdaQueryWrapper<MerchantInfo>().in(MerchantInfo::getId, merchantIds))
                .forEach(merchant -> {
                    if (!merchantNameMap.containsKey(merchant.getId())
                            && merchant.getMerchantName() != null
                            && !merchant.getMerchantName().isBlank()) {
                        merchantNameMap.put(merchant.getId(), merchant.getMerchantName());
                    }
                });
        return merchantNameMap;
    }

    private Map<Long, String> loadCategoryNameMap(Set<Long> categoryIds) {
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (categoryIds.isEmpty()) return categoryNameMap;
        productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>().in(ProductCategory::getId, categoryIds))
                .forEach(category -> categoryNameMap.put(category.getId(), category.getCategoryName()));
        return categoryNameMap;
    }

    private UnsalableProductVO toVO(ProductInfo product,
                                    Map<Long, String> merchantNameMap,
                                    Map<Long, String> categoryNameMap) {
        UnsalableProductVO vo = new UnsalableProductVO();
        vo.setId(product.getId());
        vo.setMerchantId(product.getMerchantId());
        vo.setProductName(product.getProductName());
        vo.setPrice(product.getPrice());
        vo.setSalesVolume(defaultInt(product.getSalesVolume()));
        vo.setStock(defaultInt(product.getStock()));
        vo.setProductImg(product.getProductImg());
        vo.setOriginPlace(product.getOriginPlace());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryNameMap.get(product.getCategoryId()));
        vo.setMerchantName(merchantNameMap.get(product.getMerchantId()));
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private AlgorithmDecision evaluate(ProductInfo product) {
        int stock = defaultInt(product.getStock());
        int sales = defaultInt(product.getSalesVolume());
        if (stock <= 0) {
            return new AlgorithmDecision(false, 0, 0L);
        }

        LocalDateTime createdAt = product.getCreateTime() == null ? LocalDateTime.now() : product.getCreateTime();
        long ageDays = Math.max(0L, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));

        int ageScore = (int) Math.min(ageDays, 60);
        int stockScore = Math.min(stock, 200) / 2;
        int slowSalesBonus = Math.max(0, 18 - Math.min(sales, 18));
        int staleBonus = ageDays >= 30 ? 20 : (ageDays >= 14 ? 10 : 0);
        int inventoryPressureBonus = stock > Math.max(sales * 3, 20) ? 18 : 0;
        int penalty = Math.min(sales * 2, 36);
        int score = Math.max(0, ageScore + stockScore + slowSalesBonus + staleBonus + inventoryPressureBonus - penalty);

        boolean included = score >= 45
                || (ageDays >= 14 && stock >= 20 && sales <= 10)
                || (ageDays >= 30 && stock >= 10 && sales <= 20);
        return new AlgorithmDecision(included, score, ageDays);
    }

    private String buildReason(boolean manualIncluded, boolean algorithmIncluded, AlgorithmDecision decision, ProductInfo product) {
        String metrics = String.format("上架%s天，库存%s件，累计销量%s件",
                decision.ageDays(),
                defaultInt(product.getStock()),
                defaultInt(product.getSalesVolume()));
        if (manualIncluded && algorithmIncluded) {
            return "管理员已手动加入，且算法判定该商品周转偏慢，" + metrics;
        }
        if (manualIncluded) {
            return "管理员手动加入帮扶池，" + metrics;
        }
        return "算法实时判定该商品周转偏慢，" + metrics;
    }

    private boolean matchesKeyword(UnsalableProductVO vo, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(vo.getProductName(), normalized)
                || contains(vo.getMerchantName(), normalized)
                || contains(vo.getOriginPlace(), normalized)
                || contains(vo.getCategoryName(), normalized);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesSource(UnsalableProductVO vo, String sourceType) {
        if (sourceType == null || sourceType.isBlank() || "all".equalsIgnoreCase(sourceType)) {
            return true;
        }
        return switch (sourceType) {
            case "manual" -> Boolean.TRUE.equals(vo.getManualIncluded());
            case "algorithm" -> Boolean.TRUE.equals(vo.getAlgorithmIncluded());
            case "mixed" -> Boolean.TRUE.equals(vo.getManualIncluded()) && Boolean.TRUE.equals(vo.getAlgorithmIncluded());
            default -> true;
        };
    }

    private void sortRecords(List<UnsalableProductVO> records, String sortBy) {
        Comparator<UnsalableProductVO> comparator;
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(UnsalableProductVO::getPrice, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(UnsalableProductVO::getUnsalableScore, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(UnsalableProductVO::getPrice, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(UnsalableProductVO::getUnsalableScore, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("sales".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(UnsalableProductVO::getSalesVolume, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(UnsalableProductVO::getUnsalableScore, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("stock".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(UnsalableProductVO::getStock, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(UnsalableProductVO::getUnsalableScore, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("latest".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(UnsalableProductVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            comparator = Comparator
                    .comparingInt(this::sourceWeight).reversed()
                    .thenComparing(UnsalableProductVO::getUnsalableScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(UnsalableProductVO::getStock, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(UnsalableProductVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        records.sort(comparator);
    }

    private int sourceWeight(UnsalableProductVO record) {
        boolean manualIncluded = Boolean.TRUE.equals(record.getManualIncluded());
        boolean algorithmIncluded = Boolean.TRUE.equals(record.getAlgorithmIncluded());
        if (manualIncluded && algorithmIncluded) return 3;
        if (manualIncluded) return 2;
        if (algorithmIncluded) return 1;
        return 0;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private PageResult<UnsalableProductVO> loadList(String keyword, String sortBy, String sourceType, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        List<UnsalableProductVO> source = buildRecords(keyword, sourceType);
        sortRecords(source, sortBy);

        int fromIndex = Math.min((pageNum - 1) * pageSize, source.size());
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        return PageResult.of((long) source.size(), new ArrayList<>(source.subList(fromIndex, toIndex)));
    }

    private UnsalableSummaryVO loadSummary(String keyword) {
        List<UnsalableProductVO> source = buildRecords(keyword, "all");
        UnsalableSummaryVO summary = new UnsalableSummaryVO();
        summary.setTotalCount(source.size());
        summary.setManualCount((int) source.stream().filter(item -> Boolean.TRUE.equals(item.getManualIncluded())).count());
        summary.setAlgorithmCount((int) source.stream().filter(item -> Boolean.TRUE.equals(item.getAlgorithmIncluded())).count());
        summary.setMixedCount((int) source.stream().filter(item ->
                Boolean.TRUE.equals(item.getManualIncluded()) && Boolean.TRUE.equals(item.getAlgorithmIncluded())).count());
        return summary;
    }

    private String normalize(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "blank" : text.replace(":", "_");
    }

    private record AlgorithmDecision(boolean included, int score, long ageDays) {
    }
}
