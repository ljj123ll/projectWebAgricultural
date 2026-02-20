package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;

    public com.agricultural.assistplatform.common.PageResult<AfterSaleVO> list(Integer afterSaleStatus, Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getMerchantId, merchantId);
        if (afterSaleStatus != null) q.eq(AfterSale::getAfterSaleStatus, afterSaleStatus);
        q.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AfterSaleVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
    }

    public void handle(Long id, Map<String, Object> body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id).eq(AfterSale::getMerchantId, merchantId));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        Boolean agree = body != null && body.get("agree") != null ? (Boolean) body.get("agree") : null;
        String handleResult = body != null && body.get("handleResult") != null ? body.get("handleResult").toString() : null;
        if (agree != null) {
            as.setAfterSaleStatus(agree ? 3 : 5);
            as.setHandleResult(handleResult);
        }
        afterSaleMapper.updateById(as);
    }

    private AfterSaleVO toVO(AfterSale a) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setId(a.getId());
        vo.setAfterSaleNo(a.getAfterSaleNo());
        vo.setOrderNo(a.getOrderNo());
        vo.setAfterSaleType(a.getAfterSaleType());
        vo.setApplyReason(a.getApplyReason());
        vo.setProofImgUrls(a.getProofImgUrls());
        vo.setAfterSaleStatus(a.getAfterSaleStatus());
        vo.setHandleResult(a.getHandleResult());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
