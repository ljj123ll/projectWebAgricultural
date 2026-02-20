package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.AfterSaleApplyDTO;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMainMapper orderMainMapper;

    public void apply(AfterSaleApplyDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, dto.getOrderNo()).eq(OrderMain::getUserId, userId));
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (order.getOrderStatus() != 3 && order.getOrderStatus() != 4)
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待收货/已完成订单可申请售后");
        AfterSale as = new AfterSale();
        as.setAfterSaleNo("AS" + System.currentTimeMillis());
        as.setOrderNo(dto.getOrderNo());
        as.setUserId(userId);
        as.setMerchantId(order.getMerchantId());
        as.setAfterSaleType(dto.getAfterSaleType());
        as.setApplyReason(dto.getApplyReason());
        as.setProofImgUrls(dto.getProofImgUrls());
        as.setAfterSaleStatus(1);
        afterSaleMapper.insert(as);
    }

    public com.agricultural.assistplatform.common.PageResult<AfterSaleVO> list(Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getUserId, userId).orderByDesc(AfterSale::getCreateTime));
        List<AfterSaleVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
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
