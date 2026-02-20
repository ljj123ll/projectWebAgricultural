package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.vo.admin.DashboardVO;
import com.agricultural.assistplatform.vo.admin.MerchantRankVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderMainMapper orderMainMapper;
    private final MerchantInfoMapper merchantInfoMapper;

    public DashboardVO dashboard() {
        Long totalOrderCount = orderMainMapper.selectCount(
                new LambdaQueryWrapper<OrderMain>().in(OrderMain::getOrderStatus, 2, 3, 4));
        List<OrderMain> orders = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>().in(OrderMain::getOrderStatus, 2, 3, 4));
        BigDecimal totalRevenue = orders.stream().map(OrderMain::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MerchantRankVO> rank = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>().in(OrderMain::getOrderStatus, 2, 3, 4))
                .stream()
                .collect(Collectors.groupingBy(OrderMain::getMerchantId))
                .entrySet().stream()
                .map(e -> {
                    MerchantInfo m = merchantInfoMapper.selectById(e.getKey());
                    int sales = e.getValue().size();
                    BigDecimal rev = e.getValue().stream().map(OrderMain::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    MerchantRankVO v = new MerchantRankVO();
                    v.setMerchantId(e.getKey());
                    v.setMerchantName(m != null ? m.getMerchantName() : "");
                    v.setSalesVolume(sales);
                    v.setRevenue(rev);
                    return v;
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .limit(10)
                .collect(Collectors.toList());
        DashboardVO vo = new DashboardVO();
        vo.setTotalOrderCount(totalOrderCount);
        vo.setTotalRevenue(totalRevenue);
        vo.setMerchantRank(rank);
        return vo;
    }

    public List<MerchantRankVO> merchantRank(Integer limit) {
        if (limit == null || limit < 1) limit = 10;
        return orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>().in(OrderMain::getOrderStatus, 2, 3, 4))
                .stream()
                .collect(Collectors.groupingBy(OrderMain::getMerchantId))
                .entrySet().stream()
                .map(e -> {
                    MerchantInfo m = merchantInfoMapper.selectById(e.getKey());
                    int sales = e.getValue().size();
                    BigDecimal rev = e.getValue().stream().map(OrderMain::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    MerchantRankVO v = new MerchantRankVO();
                    v.setMerchantId(e.getKey());
                    v.setMerchantName(m != null ? m.getMerchantName() : "");
                    v.setSalesVolume(sales);
                    v.setRevenue(rev);
                    return v;
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
