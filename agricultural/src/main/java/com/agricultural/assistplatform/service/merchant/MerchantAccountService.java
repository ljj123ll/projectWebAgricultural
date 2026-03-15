package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantAccount;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantAccountService {

    private final MerchantAccountMapper merchantAccountMapper;

    public List<MerchantAccount> list() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return merchantAccountMapper.selectList(new LambdaQueryWrapper<MerchantAccount>().eq(MerchantAccount::getMerchantId, merchantId));
    }

    public void save(MerchantAccount body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        MerchantAccount exist = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getMerchantId, merchantId)
                .eq(MerchantAccount::getAccountType, body.getAccountType()));
        if (exist == null) {
            MerchantAccount a = new MerchantAccount();
            a.setMerchantId(merchantId);
            a.setAccountType(body.getAccountType());
            a.setAccountNo(body.getAccountNo());
            a.setAccountName(body.getAccountName());
            a.setVerifyStatus(body.getVerifyStatus());
            a.setAuditStatus(body.getAuditStatus());
            merchantAccountMapper.insert(a);
        } else {
            exist.setAccountNo(body.getAccountNo());
            exist.setAccountName(body.getAccountName());
            exist.setVerifyStatus(body.getVerifyStatus());
            exist.setAuditStatus(body.getAuditStatus());
            merchantAccountMapper.updateById(exist);
        }
    }
}
