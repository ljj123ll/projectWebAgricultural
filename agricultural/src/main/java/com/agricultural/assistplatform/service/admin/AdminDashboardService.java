package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.UserInfo;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.UserInfoMapper;
import com.agricultural.assistplatform.vo.admin.DashboardVO;
import com.agricultural.assistplatform.vo.admin.MerchantRankVO;
import com.agricultural.assistplatform.vo.admin.RecentOrderVO;
import com.agricultural.assistplatform.vo.admin.SystemNoticeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderMainMapper orderMainMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final UserInfoMapper userInfoMapper;

    public DashboardVO dashboard(String timeRange) {
        DashboardVO vo = new DashboardVO();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = null;
        if ("week".equals(timeRange)) {
            startTime = now.minusDays(7);
        } else if ("month".equals(timeRange)) {
            startTime = now.minusDays(30);
        }
        
        // 1. 总用户数 (根据时间过滤注册)
        LambdaQueryWrapper<UserInfo> userQuery = new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getDeleteFlag, 0);
        if (startTime != null) userQuery.ge(UserInfo::getCreateTime, startTime);
        Long totalUserCount = userInfoMapper.selectCount(userQuery);
        vo.setTotalUserCount(totalUserCount);
        
        // 2. 总商家数
        LambdaQueryWrapper<MerchantInfo> merchantQuery = new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getDeleteFlag, 0);
        if (startTime != null) merchantQuery.ge(MerchantInfo::getCreateTime, startTime);
        Long totalMerchantCount = merchantInfoMapper.selectCount(merchantQuery);
        vo.setTotalMerchantCount(totalMerchantCount);
        
        // 3. 总订单数和总交易额
        LambdaQueryWrapper<OrderMain> orderQuery = new LambdaQueryWrapper<OrderMain>().in(OrderMain::getOrderStatus, 2, 3, 4);
        if (startTime != null) orderQuery.ge(OrderMain::getCreateTime, startTime);
        List<OrderMain> allOrders = orderMainMapper.selectList(orderQuery);
        vo.setTotalOrderCount((long) allOrders.size());
        BigDecimal totalRevenue = allOrders.stream()
                .map(OrderMain::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalRevenue(totalRevenue);
        
        // 4. 今日交易额
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<OrderMain> todayOrders = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>()
                        .in(OrderMain::getOrderStatus, 2, 3, 4)
                        .ge(OrderMain::getCreateTime, todayStart));
        BigDecimal todayRevenue = todayOrders.stream()
                .map(OrderMain::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayRevenue(todayRevenue);
        
        // 5. 待审核数量（商家待审核 + 商品待审核）
        Long pendingMerchantCount = merchantInfoMapper.selectCount(
                new LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getAuditStatus, 0)
                        .eq(MerchantInfo::getDeleteFlag, 0));
        vo.setPendingAuditCount(pendingMerchantCount);
        
        // 6. 商家销量排行 TOP10
        List<MerchantRankVO> rank = allOrders.stream()
                .collect(Collectors.groupingBy(OrderMain::getMerchantId))
                .entrySet().stream()
                .map(e -> {
                    MerchantInfo m = merchantInfoMapper.selectById(e.getKey());
                    int sales = e.getValue().size();
                    BigDecimal rev = e.getValue().stream()
                            .map(OrderMain::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        vo.setMerchantRank(rank);
        
        // 7. 最近订单（最近5条）
        List<OrderMain> recentOrderList = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>()
                        .orderByDesc(OrderMain::getCreateTime)
                        .last("LIMIT 5"));
        List<RecentOrderVO> recentOrders = recentOrderList.stream().map(o -> {
            RecentOrderVO rv = new RecentOrderVO();
            rv.setOrderId(o.getId());
            rv.setOrderNo(o.getOrderNo());
            MerchantInfo m = merchantInfoMapper.selectById(o.getMerchantId());
            rv.setMerchantName(m != null ? m.getMerchantName() : "");
            rv.setOrderAmount(o.getTotalAmount());
            rv.setOrderStatus(o.getOrderStatus());
            rv.setCreateTime(o.getCreateTime());
            return rv;
        }).collect(Collectors.toList());
        vo.setRecentOrders(recentOrders);
        
        // 8. 系统公告（模拟数据）
        List<SystemNoticeVO> notices = new ArrayList<>();
        SystemNoticeVO notice1 = new SystemNoticeVO();
        notice1.setId(1L);
        notice1.setTitle("系统运行正常");
        notice1.setContent("所有服务运行正常，无异常报警。");
        notice1.setCreateTime(LocalDateTime.now());
        notices.add(notice1);
        vo.setNotices(notices);
        
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
