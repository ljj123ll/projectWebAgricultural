package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.vo.merchant.HotProductVO;
import com.agricultural.assistplatform.vo.merchant.MerchantStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantStatsService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductInfoMapper productInfoMapper;
    private final com.agricultural.assistplatform.mapper.ReconciliationDetailMapper reconciliationDetailMapper;
    private final com.agricultural.assistplatform.mapper.SubsidyDetailMapper subsidyDetailMapper;

    public MerchantStatsVO stats() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        List<OrderMain> orders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getMerchantId, merchantId).in(OrderMain::getOrderStatus, 2, 3, 4)
                .ge(OrderMain::getCreateTime, start));
        int salesCount7d = 0;
        BigDecimal revenue7d = BigDecimal.ZERO;
        Map<Long, Integer> productSales = new HashMap<>();
        for (OrderMain o : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, o.getOrderNo()));
            for (OrderItem item : items) {
                salesCount7d += item.getProductNum();
                revenue7d = revenue7d.add(item.getProductAmount());
                productSales.merge(item.getProductId(), item.getProductNum(), Integer::sum);
            }
        }
        List<HotProductVO> hot = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    ProductInfo p = productInfoMapper.selectById(e.getKey());
                    HotProductVO v = new HotProductVO();
                    v.setProductId(e.getKey());
                    v.setProductName(p != null ? p.getProductName() : "");
                    v.setSalesVolume(e.getValue());
                    return v;
                })
                .collect(Collectors.toList());
        MerchantStatsVO vo = new MerchantStatsVO();
        vo.setSalesCount7d(salesCount7d);
        vo.setRevenue7d(revenue7d);
        vo.setHotProducts(hot);
        return vo;
    }

    public com.agricultural.assistplatform.common.PageResult<com.agricultural.assistplatform.entity.ReconciliationDetail> reconciliation(Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.agricultural.assistplatform.entity.ReconciliationDetail> page =
                reconciliationDetailMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<com.agricultural.assistplatform.entity.ReconciliationDetail>()
                                .eq(com.agricultural.assistplatform.entity.ReconciliationDetail::getMerchantId, merchantId)
                                .orderByDesc(com.agricultural.assistplatform.entity.ReconciliationDetail::getCreateTime));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    public com.agricultural.assistplatform.common.PageResult<com.agricultural.assistplatform.entity.SubsidyDetail> subsidy(Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.agricultural.assistplatform.entity.SubsidyDetail> page =
                subsidyDetailMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<com.agricultural.assistplatform.entity.SubsidyDetail>()
                                .eq(com.agricultural.assistplatform.entity.SubsidyDetail::getMerchantId, merchantId)
                                .orderByDesc(com.agricultural.assistplatform.entity.SubsidyDetail::getCreateTime));
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }
}
