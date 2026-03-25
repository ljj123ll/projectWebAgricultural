package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMerchantAccountService {

    private final MerchantAccountMapper merchantAccountMapper;

    public PageResult<MerchantAccount> list(Integer auditStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<MerchantAccount> q = new LambdaQueryWrapper<>();
        if (auditStatus != null) q.eq(MerchantAccount::getAuditStatus, auditStatus);
        q.orderByAsc(MerchantAccount::getAuditStatus).orderByDesc(MerchantAccount::getUpdateTime);
        Page<MerchantAccount> page = merchantAccountMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Boolean approve, String reason) {
        if (id == null) throw new BusinessException(ResultCode.BAD_REQUEST, "账户ID不能为空");
        if (approve == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请明确审核结果");
        MerchantAccount account = merchantAccountMapper.selectById(id);
        if (account == null) throw new BusinessException(ResultCode.NOT_FOUND, "账户不存在");
        if (approve) {
            if (account.getVerifyStatus() == null || account.getVerifyStatus() != 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该账户尚未完成1分钱验证，不能审核通过");
            }
            account.setAuditStatus(1);
            account.setRejectReason(null);
        } else {
            account.setAuditStatus(2);
            account.setRejectReason(reason == null ? "资料信息不完整，请补充后重新提交" : reason.trim());
        }
        merchantAccountMapper.updateById(account);
    }
}

