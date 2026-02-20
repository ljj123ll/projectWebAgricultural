package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final MerchantInfoMapper merchantInfoMapper;
    private final ProductInfoMapper productInfoMapper;

    public com.agricultural.assistplatform.common.PageResult<MerchantInfo> merchantAuditList(Integer auditStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<MerchantInfo> q = new LambdaQueryWrapper<MerchantInfo>();
        if (auditStatus != null) q.eq(MerchantInfo::getAuditStatus, auditStatus);
        q.orderByDesc(MerchantInfo::getCreateTime);
        Page<MerchantInfo> page = merchantInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditMerchant(Long id, Map<String, Object> body) {
        Boolean pass = body != null && body.get("pass") != null ? (Boolean) body.get("pass") : null;
        String reason = body != null && body.get("rejectReason") != null ? body.get("rejectReason").toString() : null;
        if (pass == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请传入 pass");
        MerchantInfo m = merchantInfoMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        m.setAuditStatus(pass ? 1 : 2);
        m.setRejectReason(pass ? null : reason);
        if (pass) m.setStatus(1);
        merchantInfoMapper.updateById(m);
    }

    public com.agricultural.assistplatform.common.PageResult<ProductInfo> productAuditList(Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<ProductInfo> q = new LambdaQueryWrapper<ProductInfo>();
        if (status != null) q.eq(ProductInfo::getStatus, status);
        q.orderByDesc(ProductInfo::getCreateTime);
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(Long id, Map<String, Object> body) {
        Boolean pass = body != null && body.get("pass") != null ? (Boolean) body.get("pass") : null;
        String reason = body != null && body.get("rejectReason") != null ? body.get("rejectReason").toString() : null;
        if (pass == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请传入 pass");
        ProductInfo p = productInfoMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        p.setStatus(pass ? 1 : 3);
        p.setRejectReason(pass ? null : reason);
        productInfoMapper.updateById(p);
    }
}
