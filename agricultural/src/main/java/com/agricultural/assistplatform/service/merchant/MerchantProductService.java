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
import com.agricultural.assistplatform.vo.merchant.MerchantProductDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantProductService {

    private final ProductInfoMapper productInfoMapper;
    private final ProductTraceMapper productTraceMapper;
    private final QrCodeUtil qrCodeUtil;
    private final UserRealtimeEventService userRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final PublicDataCacheService publicDataCacheService;

    @Value("${app.trace.base-url:http://localhost:5173/product}")
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
            vo.setPlantingCycle(trace.getPlantingCycle());
            vo.setOriginPlaceDetail(trace.getOriginPlaceDetail());
            vo.setFertilizerType(trace.getFertilizerType());
            vo.setStorageMethod(trace.getStorageMethod());
            vo.setTransportMethod(trace.getTransportMethod());
            vo.setQrCodeUrl(trace.getQrCodeUrl());
        }
        return vo;
    }

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
        if (trace != null) {
            trace.setPlantingCycle(dto.getPlantingCycle());
            trace.setOriginPlaceDetail(dto.getOriginPlaceDetail());
            trace.setFertilizerType(dto.getFertilizerType());
            trace.setStorageMethod(dto.getStorageMethod());
            trace.setTransportMethod(dto.getTransportMethod());
            productTraceMapper.updateById(trace);
        }
        publicDataCacheService.refreshHotProduct(p);
        publicDataCacheService.evictProductCatalog(id, merchantId);
        userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
        adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
    }

    public String generateQrcode(Long productId) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        String url = buildTraceUrl(productId);
        String qrUrl = qrCodeUtil.generateAndSave(url, "trace_" + productId);
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>().eq(ProductTrace::getProductId, productId));
        if (trace != null) {
            trace.setQrCodeUrl(qrUrl);
            productTraceMapper.updateById(trace);
        }
        publicDataCacheService.evictProductDetail(productId);
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

    private String buildTraceUrl(Long productId) {
        String baseUrl = traceBaseUrl == null ? "" : traceBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + productId;
        }
        return baseUrl + "/" + productId;
    }
}
