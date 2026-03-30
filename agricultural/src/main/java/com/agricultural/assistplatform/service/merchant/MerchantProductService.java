package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.merchant.ProductSaveDTO;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ProductTrace;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ProductTraceMapper;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.util.QrCodeUtil;
import com.agricultural.assistplatform.util.TraceabilityCatalog;
import com.agricultural.assistplatform.vo.merchant.MerchantProductDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 商家商品服务。
 * 负责商品发布、编辑、上下架，以及商品溯源码和二维码档案的生成维护。
 */
public class MerchantProductService {

    private final ProductInfoMapper productInfoMapper;
    private final ProductTraceMapper productTraceMapper;
    private final QrCodeUtil qrCodeUtil;
    private final UserRealtimeEventService userRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final PublicDataCacheService publicDataCacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.trace.base-url:http://localhost:5173/trace}")
    private String traceBaseUrl;

    public com.agricultural.assistplatform.common.PageResult<ProductInfo> list(Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ProductInfo>().eq(ProductInfo::getMerchantId, merchantId).orderByDesc(ProductInfo::getCreateTime));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    /**
     * 商家商品详情，用于编辑页回填表单和溯源档案。
     */
    public MerchantProductDetailVO detail(Long id) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, id).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, id));
        MerchantProductDetailVO vo = new MerchantProductDetailVO();
        vo.setId(p.getId());
        vo.setProductName(p.getProductName());
        vo.setCategoryId(p.getCategoryId());
        vo.setPrice(p.getPrice());
        vo.setStock(p.getStock());
        vo.setStockWarning(p.getStockWarning());
        vo.setProductImg(p.getProductImg());
        vo.setProductDetailImg(p.getProductDetailImg());
        vo.setProductDesc(p.getProductDesc());
        vo.setOriginPlace(p.getOriginPlace());
        vo.setStatus(p.getStatus());
        if (trace != null) {
            vo.setTraceCode(trace.getTraceCode());
            vo.setBatchNo(trace.getBatchNo());
            vo.setProductionDate(trace.getProductionDate());
            vo.setHarvestDate(trace.getHarvestDate());
            vo.setPackagingDate(trace.getPackagingDate());
            vo.setInspectionReport(trace.getInspectionReport());
            vo.setPlantingCycle(trace.getPlantingCycle());
            vo.setOriginPlaceDetail(trace.getOriginPlaceDetail());
            vo.setFertilizerType(trace.getFertilizerType());
            vo.setStorageMethod(trace.getStorageMethod());
            vo.setTransportMethod(trace.getTransportMethod());
            vo.setQrCodeUrl(trace.getQrCodeUrl());
            vo.setTraceExtra(parseTraceExtra(p.getCategoryId(), trace.getTraceExtra()));
        }
        return vo;
    }

    /**
     * 新增商品。
     * 会同时创建商品基础信息和对应的溯源档案记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductInfo add(ProductSaveDTO dto) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = new ProductInfo();
        p.setMerchantId(merchantId);
        p.setProductName(dto.getProductName());
        p.setCategoryId(dto.getCategoryId());
        p.setPrice(dto.getPrice());
        p.setStock(dto.getStock() != null ? dto.getStock() : 0);
        p.setStockWarning(dto.getStockWarning() != null ? dto.getStockWarning() : 10);
        p.setProductImg(dto.getProductImg());
        p.setProductDetailImg(dto.getProductDetailImg());
        p.setProductDesc(dto.getProductDesc());
        p.setOriginPlace(dto.getOriginPlace());
        p.setStatus(0);
        p.setSalesVolume(0);
        p.setScore(java.math.BigDecimal.valueOf(5));
        productInfoMapper.insert(p);
        ProductTrace trace = new ProductTrace();
        trace.setProductId(p.getId());
        trace.setTraceCode(generateTraceCode());
        trace.setBatchNo(dto.getBatchNo());
        trace.setProductionDate(dto.getProductionDate());
        trace.setHarvestDate(dto.getHarvestDate());
        trace.setPackagingDate(dto.getPackagingDate());
        trace.setInspectionReport(dto.getInspectionReport());
        trace.setTraceExtra(writeTraceExtra(dto.getCategoryId(), dto.getTraceExtra()));
        trace.setPlantingCycle(dto.getPlantingCycle());
        trace.setOriginPlaceDetail(dto.getOriginPlaceDetail());
        trace.setFertilizerType(dto.getFertilizerType());
        trace.setStorageMethod(dto.getStorageMethod());
        trace.setTransportMethod(dto.getTransportMethod());
        productTraceMapper.insert(trace);
        publicDataCacheService.refreshHotProduct(p);
        publicDataCacheService.evictProductCatalog(p.getId(), merchantId);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(p.getId()));
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(p.getId()));
        return p;
    }

    /**
     * 编辑商品，同时维护基础溯源字段和分类扩展字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveDTO dto) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, id).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        p.setProductName(dto.getProductName());
        p.setCategoryId(dto.getCategoryId());
        p.setPrice(dto.getPrice());
        if (dto.getStock() != null) p.setStock(dto.getStock());
        if (dto.getStockWarning() != null) p.setStockWarning(dto.getStockWarning());
        p.setProductImg(dto.getProductImg());
        p.setProductDetailImg(dto.getProductDetailImg());
        p.setProductDesc(dto.getProductDesc());
        p.setOriginPlace(dto.getOriginPlace());
        productInfoMapper.updateById(p);
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>().eq(ProductTrace::getProductId, id));
        if (trace == null) {
            trace = new ProductTrace();
            trace.setProductId(id);
            trace.setTraceCode(generateTraceCode());
            fillTrace(trace, dto);
            productTraceMapper.insert(trace);
        } else {
            fillTrace(trace, dto);
            if (trace.getTraceCode() == null || trace.getTraceCode().isBlank()) {
                trace.setTraceCode(generateTraceCode());
            }
            productTraceMapper.updateById(trace);
        }
        publicDataCacheService.evictProductDetail(id);
        publicDataCacheService.evictTraceArchive(trace.getTraceCode());
        publicDataCacheService.refreshHotProduct(p);
        publicDataCacheService.evictProductCatalog(id, merchantId);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
    }

    /**
     * 生成当前商品的二维码，二维码指向独立溯源档案页。
     */
    public String generateQrcode(Long productId) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>().eq(ProductTrace::getProductId, productId));
        if (trace == null) {
            trace = new ProductTrace();
            trace.setProductId(productId);
            trace.setTraceCode(generateTraceCode());
            productTraceMapper.insert(trace);
        }
        if (trace.getTraceCode() == null || trace.getTraceCode().isBlank()) {
            trace.setTraceCode(generateTraceCode());
        }
        String url = buildTraceUrl(trace.getTraceCode());
        String qrUrl = qrCodeUtil.generateAndSave(url, "trace_" + productId);
        if (qrUrl == null || qrUrl.isBlank()) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "溯源码生成失败");
        }
        trace.setQrCodeUrl(qrUrl);
        productTraceMapper.updateById(trace);
        publicDataCacheService.evictProductDetail(productId);
        publicDataCacheService.evictTraceArchive(trace.getTraceCode());
        return qrUrl;
    }

    public void updateStatus(Long productId, Integer status) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        if (status != 1 && status != 2) throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能为上架(1)或下架(2)");
        if (status == 1 && p.getStatus() != 1) {
            if (p.getStatus() == 0) throw new BusinessException(ResultCode.BAD_REQUEST, "商品待审核通过后才能上架");
        }
        p.setStatus(status);
        productInfoMapper.updateById(p);
        publicDataCacheService.refreshHotProduct(p);
        publicDataCacheService.evictProductCatalog(productId, merchantId);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(productId));
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(productId));
    }

    public void delete(Long productId) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        productInfoMapper.deleteById(productId);
        publicDataCacheService.removeHotProduct(productId);
        publicDataCacheService.evictProductCatalog(productId, merchantId);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(productId));
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(productId));
    }

    private String buildTraceUrl(String traceCode) {
        String baseUrl = traceBaseUrl == null ? "" : traceBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + traceCode;
        }
        return baseUrl + "/" + traceCode;
    }

    /**
     * 回填或更新溯源档案实体。
     */
    private void fillTrace(ProductTrace trace, ProductSaveDTO dto) {
        trace.setBatchNo(dto.getBatchNo());
        trace.setProductionDate(dto.getProductionDate());
        trace.setHarvestDate(dto.getHarvestDate());
        trace.setPackagingDate(dto.getPackagingDate());
        trace.setInspectionReport(dto.getInspectionReport());
        trace.setTraceExtra(writeTraceExtra(dto.getCategoryId(), dto.getTraceExtra()));
        trace.setPlantingCycle(dto.getPlantingCycle());
        trace.setOriginPlaceDetail(dto.getOriginPlaceDetail());
        trace.setFertilizerType(dto.getFertilizerType());
        trace.setStorageMethod(dto.getStorageMethod());
        trace.setTransportMethod(dto.getTransportMethod());
    }

    private String generateTraceCode() {
        return "TRACE-" + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 将分类溯源扩展字段规范化后写入 JSON，避免无关字段混入数据库。
     */
    private String writeTraceExtra(Long categoryId, Map<String, String> traceExtra) {
        try {
            Map<String, String> sanitized = TraceabilityCatalog.sanitize(categoryId, traceExtra);
            boolean hasValue = sanitized.values().stream().anyMatch(value -> value != null && !value.isBlank());
            return hasValue ? objectMapper.writeValueAsString(sanitized) : null;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "溯源扩展字段保存失败");
        }
    }

    private Map<String, String> parseTraceExtra(Long categoryId, String traceExtra) {
        if (traceExtra == null || traceExtra.isBlank()) {
            return TraceabilityCatalog.sanitize(categoryId, null);
        }
        try {
            Map<String, String> raw = objectMapper.readValue(traceExtra, new TypeReference<>() {
            });
            return new LinkedHashMap<>(TraceabilityCatalog.sanitize(categoryId, raw));
        } catch (Exception e) {
            return new LinkedHashMap<>(TraceabilityCatalog.sanitize(categoryId, null));
        }
    }
}
