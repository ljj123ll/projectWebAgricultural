package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.merchant.ProductSaveDTO;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ProductTrace;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ProductTraceMapper;
import com.agricultural.assistplatform.util.QrCodeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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

    public com.agricultural.assistplatform.common.PageResult<ProductInfo> list(Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ProductInfo>().eq(ProductInfo::getMerchantId, merchantId).orderByDesc(ProductInfo::getCreateTime));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
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
    }

    public String generateQrcode(Long productId) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ProductInfo p = productInfoMapper.selectOne(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getId, productId).eq(ProductInfo::getMerchantId, merchantId));
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        String url = "https://your-domain.com/trace/" + productId;
        String qrUrl = qrCodeUtil.generateAndSave(url, "trace_" + productId);
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>().eq(ProductTrace::getProductId, productId));
        if (trace != null) {
            trace.setQrCodeUrl(qrUrl);
            productTraceMapper.updateById(trace);
        }
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
    }
}
