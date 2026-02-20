package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.LogisticsInfoMapper;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.vo.merchant.MerchantOrderVO;
import com.agricultural.assistplatform.vo.user.OrderItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantOrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final LogisticsInfoMapper logisticsInfoMapper;

    public com.agricultural.assistplatform.common.PageResult<MerchantOrderVO> list(Integer orderStatus, Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<OrderMain> q = new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getMerchantId, merchantId);
        if (orderStatus != null) q.eq(OrderMain::getOrderStatus, orderStatus);
        q.orderByDesc(OrderMain::getCreateTime);
        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<MerchantOrderVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, Map<String, String> body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain main = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getId, id).eq(OrderMain::getMerchantId, merchantId));
        if (main == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (main.getOrderStatus() != 2) throw new BusinessException(ResultCode.BAD_REQUEST, "仅待发货订单可填写物流");
        String company = body.get("logisticsCompany");
        String no = body.get("logisticsNo");
        if (company == null || no == null || no.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "请填写快递公司与物流单号");
        main.setOrderStatus(3);
        orderMainMapper.updateById(main);
        LogisticsInfo log = logisticsInfoMapper.selectOne(new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
        if (log != null) {
            log.setLogisticsCompany(company);
            log.setLogisticsNo(no);
            log.setLogisticsStatus(1);
            logisticsInfoMapper.updateById(log);
        } else {
            LogisticsInfo newLog = new LogisticsInfo();
            newLog.setOrderNo(main.getOrderNo());
            newLog.setLogisticsCompany(company);
            newLog.setLogisticsNo(no);
            newLog.setLogisticsStatus(1);
            logisticsInfoMapper.insert(newLog);
        }
    }

    private MerchantOrderVO toVO(OrderMain main) {
        MerchantOrderVO vo = new MerchantOrderVO();
        vo.setId(main.getId());
        vo.setOrderNo(main.getOrderNo());
        vo.setTotalAmount(main.getTotalAmount());
        vo.setOrderStatus(main.getOrderStatus());
        vo.setReceiver(main.getReceiver());
        vo.setReceiverPhone(main.getReceiverPhone());
        vo.setReceiverAddress(main.getReceiverAddress());
        vo.setCreateTime(main.getCreateTime());
        LogisticsInfo log = logisticsInfoMapper.selectOne(new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderNo, main.getOrderNo()));
        if (log != null) {
            vo.setLogisticsCompany(log.getLogisticsCompany());
            vo.setLogisticsNo(log.getLogisticsNo());
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, main.getOrderNo()));
        vo.setItems(items.stream().map(oi -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setProductId(oi.getProductId());
            iv.setProductName(oi.getProductName());
            iv.setProductImg(oi.getProductImg());
            iv.setProductPrice(oi.getProductPrice());
            iv.setProductNum(oi.getProductNum());
            iv.setProductAmount(oi.getProductAmount());
            return iv;
        }).collect(Collectors.toList()));
        return vo;
    }
}
