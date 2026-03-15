package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminMerchantService {

    private final MerchantInfoMapper merchantInfoMapper;

    public PageResult<MerchantInfo> list(Integer auditStatus, Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<MerchantInfo> q = new LambdaQueryWrapper<MerchantInfo>();
        if (auditStatus != null) q.eq(MerchantInfo::getAuditStatus, auditStatus);
        if (status != null) q.eq(MerchantInfo::getStatus, status);
        q.orderByDesc(MerchantInfo::getCreateTime);
        Page<MerchantInfo> page = merchantInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void updateStatus(Long id, Integer status) {
        MerchantInfo m = new MerchantInfo();
        m.setId(id);
        m.setStatus(status);
        merchantInfoMapper.updateById(m);
    }
}
