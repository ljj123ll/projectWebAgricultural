package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.PaymentRecord;
import com.agricultural.assistplatform.mapper.LogisticsInfoMapper;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.PaymentRecordMapper;
import com.agricultural.assistplatform.vo.admin.AdminOrderVO;
import com.agricultural.assistplatform.vo.user.OrderItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final LogisticsInfoMapper logisticsInfoMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public PageResult<AdminOrderVO> list(Integer status, String keyword, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>();
        if (status != null) q.eq(OrderMain::getOrderStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            q.and(w -> w.like(OrderMain::getOrderNo, trimmed)
                    .or().like(OrderMain::getReceiverPhone, trimmed)
                    .or().like(OrderMain::getReceiver, trimmed));
        }
        q.orderByDesc(OrderMain::getCreateTime);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AdminOrderVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), list);
    }

    public AdminOrderVO get(Long id) {
        OrderMain main = orderMainMapper.selectById(id);
        return main == null ? null : toVO(main);
    }

    public void cancel(Long id, String reason) {
        OrderMain o = orderMainMapper.selectById(id);
        if (o == null) return;
        o.setOrderStatus(5);
        o.setCancelReason(reason);
        orderMainMapper.updateById(o);
    }

    private AdminOrderVO toVO(OrderMain main) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(main.getId());
        vo.setOrderNo(main.getOrderNo());
        vo.setUserId(main.getUserId());
        vo.setAddressId(main.getAddressId());
        vo.setTotalAmount(main.getTotalAmount());
        vo.setOrderStatus(main.getOrderStatus());
        vo.setCancelReason(main.getCancelReason());
        vo.setPayDeadline(main.getPayDeadline());
        vo.setReceiver(main.getReceiver());
        vo.setReceiverPhone(main.getReceiverPhone());
        vo.setReceiverAddress(main.getReceiverAddress());
        vo.setMerchantId(main.getMerchantId());
        vo.setCreateTime(main.getCreateTime());
        vo.setUpdateTime(main.getUpdateTime());

        LogisticsInfo logistics = logisticsInfoMapper.selectOne(
                new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
        if (logistics != null) {
            vo.setLogisticsCompany(logistics.getLogisticsCompany());
            vo.setLogisticsNo(logistics.getLogisticsNo());
            vo.setLogisticsStatus(logistics.getLogisticsStatus());
            vo.setAbnormalReason(logistics.getAbnormalReason());
        }

        List<PaymentRecord> payments = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderNo, main.getOrderNo())
                        .orderByDesc(PaymentRecord::getPayTime)
                        .orderByDesc(PaymentRecord::getId));
        if (payments != null && !payments.isEmpty()) {
            PaymentRecord successPayment = payments.stream()
                    .filter(item -> item.getPayStatus() != null && item.getPayStatus() == 1 && item.getPayTime() != null)
                    .findFirst()
                    .orElse(payments.get(0));
            vo.setPayTime(successPayment.getPayTime());
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()));
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductImg(item.getProductImg());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setProductNum(item.getProductNum());
            itemVO.setProductAmount(item.getProductAmount());
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }
}
