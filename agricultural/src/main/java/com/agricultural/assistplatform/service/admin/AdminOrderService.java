package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderMainMapper orderMainMapper;

    public PageResult<OrderMain> list(Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>();
        if (status != null) q.eq(OrderMain::getOrderStatus, status);
        q.orderByDesc(OrderMain::getCreateTime);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public OrderMain get(Long id) {
        return orderMainMapper.selectById(id);
    }

    public void cancel(Long id, String reason) {
        OrderMain o = orderMainMapper.selectById(id);
        if (o == null) return;
        o.setOrderStatus(5);
        o.setCancelReason(reason);
        orderMainMapper.updateById(o);
    }
}
