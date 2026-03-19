package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.vo.admin.AdminProductAuditVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final MerchantInfoMapper merchantInfoMapper;
    private final ProductInfoMapper productInfoMapper;
    private final ShopInfoMapper shopInfoMapper;

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

    public com.agricultural.assistplatform.common.PageResult<AdminProductAuditVO> productAuditList(Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<ProductInfo> q = new LambdaQueryWrapper<ProductInfo>();
        if (status != null) q.eq(ProductInfo::getStatus, status);
        q.orderByDesc(ProductInfo::getCreateTime);
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        Set<Long> merchantIds = page.getRecords().stream().map(ProductInfo::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantInfo> merchantMap = new HashMap<>();
        Map<Long, ShopInfo> shopMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantInfoMapper.selectList(new LambdaQueryWrapper<MerchantInfo>().in(MerchantInfo::getId, merchantIds))
                    .forEach(m -> merchantMap.put(m.getId(), m));
            shopInfoMapper.selectList(new LambdaQueryWrapper<ShopInfo>().in(ShopInfo::getMerchantId, merchantIds))
                    .forEach(s -> shopMap.put(s.getMerchantId(), s));
        }
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(),
                page.getRecords().stream().map(p -> {
                    AdminProductAuditVO vo = new AdminProductAuditVO();
                    vo.setId(p.getId());
                    vo.setProductName(p.getProductName());
                    vo.setMerchantId(p.getMerchantId());
                    MerchantInfo m = merchantMap.get(p.getMerchantId());
                    if (m != null) vo.setMerchantName(m.getMerchantName());
                    ShopInfo s = shopMap.get(p.getMerchantId());
                    if (s != null) vo.setShopName(s.getShopName());
                    vo.setCategoryId(p.getCategoryId());
                    vo.setPrice(p.getPrice());
                    vo.setStock(p.getStock());
                    vo.setProductImg(p.getProductImg());
                    vo.setProductDesc(p.getProductDesc());
                    vo.setStatus(p.getStatus());
                    vo.setRejectReason(p.getRejectReason());
                    vo.setCreateTime(p.getCreateTime());
                    return vo;
                }).collect(Collectors.toList()));
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
