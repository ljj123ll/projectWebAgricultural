package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ReconciliationDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTransferService {

    private final ReconciliationDetailMapper reconciliationDetailMapper;
    private final OrderMainMapper orderMainMapper;

    public com.agricultural.assistplatform.common.PageResult<ReconciliationDetail> transferList(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<ReconciliationDetail> page = reconciliationDetailMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ReconciliationDetail>().orderByDesc(ReconciliationDetail::getCreateTime));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    public void manualTransfer(Long id) {
        ReconciliationDetail r = reconciliationDetailMapper.selectById(id);
        if (r == null) return;
        r.setTransferStatus(1);
        r.setTransferTime(LocalDateTime.now());
        r.setTransferNo("MANUAL_" + System.currentTimeMillis());
        reconciliationDetailMapper.updateById(r);
    }

    public com.agricultural.assistplatform.common.PageResult<OrderMain> logisticsAbnormal(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LocalDateTime deadline = LocalDateTime.now().minusDays(5);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getOrderStatus, 3).lt(OrderMain::getCreateTime, deadline));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }
}
