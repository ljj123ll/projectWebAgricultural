package com.agricultural.assistplatform.util;

import com.agricultural.assistplatform.vo.user.TraceExtraFieldVO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TraceabilityCatalog {

    public static final long CATEGORY_FRESH = 1L;
    public static final long CATEGORY_GRAINS = 2L;
    public static final long CATEGORY_SPECIALTY = 3L;
    public static final long CATEGORY_LIVESTOCK = 4L;

    private static final Map<Long, LinkedHashMap<String, String>> CATEGORY_FIELD_MAP = new LinkedHashMap<>();

    static {
        CATEGORY_FIELD_MAP.put(CATEGORY_FRESH, orderedMap(
                entry("variety", "品种规格"),
                entry("plantingMethod", "种植方式"),
                entry("irrigationMethod", "灌溉方式"),
                entry("pestControl", "病虫害防治"),
                entry("pickingWindow", "采摘时段"),
                entry("qualityGrade", "品质等级")
        ));
        CATEGORY_FIELD_MAP.put(CATEGORY_GRAINS, orderedMap(
                entry("rawMaterialSource", "原粮来源"),
                entry("processingTechnology", "加工工艺"),
                entry("millingDate", "碾磨压榨日期"),
                entry("ingredientList", "配料组成"),
                entry("shelfLife", "保质周期"),
                entry("packageSpec", "规格净含量")
        ));
        CATEGORY_FIELD_MAP.put(CATEGORY_SPECIALTY, orderedMap(
                entry("specialtyType", "特产类型"),
                entry("dryingMethod", "干制方式"),
                entry("roastingLevel", "烘焙火候"),
                entry("storageHumidity", "储藏环境"),
                entry("flavorProfile", "风味特征"),
                entry("recommendedUse", "推荐吃法")
        ));
        CATEGORY_FIELD_MAP.put(CATEGORY_LIVESTOCK, orderedMap(
                entry("breed", "品种来源"),
                entry("breedingMethod", "养殖方式"),
                entry("feedType", "饲喂方式"),
                entry("quarantineStatus", "检疫情况"),
                entry("slaughterOrLayingDate", "屠宰产蛋日期"),
                entry("coldChainTemperature", "冷链温度")
        ));
    }

    private TraceabilityCatalog() {
    }

    public static Map<String, String> getFieldLabels(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyMap();
        }
        return CATEGORY_FIELD_MAP.getOrDefault(categoryId, new LinkedHashMap<>());
    }

    public static Map<String, String> sanitize(Long categoryId, Map<String, String> raw) {
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        getFieldLabels(categoryId).forEach((key, label) -> {
            String value = raw == null ? "" : String.valueOf(raw.getOrDefault(key, "")).trim();
            sanitized.put(key, value);
        });
        return sanitized;
    }

    public static List<TraceExtraFieldVO> toFieldList(Long categoryId, Map<String, String> raw) {
        return sanitize(categoryId, raw).entrySet().stream()
                .map(entry -> {
                    String label = getFieldLabels(categoryId).get(entry.getKey());
                    return new TraceExtraFieldVO(entry.getKey(), label, entry.getValue());
                })
                .filter(field -> field.getValue() != null && !field.getValue().isBlank())
                .collect(Collectors.toList());
    }

    @SafeVarargs
    private static LinkedHashMap<String, String> orderedMap(Map.Entry<String, String>... entries) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static Map.Entry<String, String> entry(String key, String label) {
        return Map.entry(Objects.requireNonNull(key), Objects.requireNonNull(label));
    }
}
