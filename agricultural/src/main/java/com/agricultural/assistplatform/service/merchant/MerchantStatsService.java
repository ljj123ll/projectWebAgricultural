package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.AfterSaleFlow;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderItem;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderItemMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.vo.merchant.DailySalesVO;
import com.agricultural.assistplatform.vo.merchant.HotProductVO;
import com.agricultural.assistplatform.vo.merchant.MerchantStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantStatsService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductInfoMapper productInfoMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final com.agricultural.assistplatform.mapper.ReconciliationDetailMapper reconciliationDetailMapper;
    private final com.agricultural.assistplatform.mapper.SubsidyDetailMapper subsidyDetailMapper;

    public MerchantStatsVO stats() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDate start7d = today.minusDays(6); // 含今天共7天
        LocalDateTime start7dTime = start7d.atStartOfDay();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();

        // 待办数据：订单（待发货）、售后（待商家处理/待商家签收退款）、库存预警
        Long pendingOrdersLong = orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getMerchantId, merchantId)
                .eq(OrderMain::getOrderStatus, 2));
        int pendingOrders = pendingOrdersLong != null ? pendingOrdersLong.intValue() : 0;

        Long pendingAfterSalesLong = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getMerchantId, merchantId)
                .in(AfterSale::getAfterSaleStatus,
                        AfterSaleFlow.STATUS_PENDING_MERCHANT,
                        AfterSaleFlow.STATUS_WAIT_MERCHANT_REFUND));
        int pendingAfterSales = pendingAfterSalesLong != null ? pendingAfterSalesLong.intValue() : 0;

        Long lowStockLong = productInfoMapper.selectCount(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getMerchantId, merchantId)
                .eq(ProductInfo::getStatus, 1) // 已上架商品
                .apply("stock < stock_warning"));
        int lowStock = lowStockLong != null ? lowStockLong.intValue() : 0;

        // 近7天销量/营收 + 趋势
        List<OrderMain> orders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getMerchantId, merchantId)
                .in(OrderMain::getOrderStatus, 2, 3, 4)
                .ge(OrderMain::getCreateTime, start7dTime)
                .orderByAsc(OrderMain::getCreateTime));

        int salesCount7d = 0;
        BigDecimal revenue7d = BigDecimal.ZERO;
        Map<Long, Integer> productSales = new HashMap<>();

        int todayOrders = 0;
        BigDecimal todaySales = BigDecimal.ZERO;
        int yesterdayOrders = 0;
        BigDecimal yesterdaySales = BigDecimal.ZERO;

        Map<LocalDate, DailySalesVO> dailyMap = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDate d = start7d.plusDays(i);
            DailySalesVO v = new DailySalesVO();
            v.setDate(d.format(fmt));
            v.setOrderCount(0);
            v.setSales(BigDecimal.ZERO);
            dailyMap.put(d, v);
        }

        for (OrderMain o : orders) {
            LocalDate d = o.getCreateTime() != null ? o.getCreateTime().toLocalDate() : null;
            DailySalesVO dayVO = d != null ? dailyMap.get(d) : null;
            if (dayVO != null) {
                dayVO.setOrderCount(dayVO.getOrderCount() + 1);
            }

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, o.getOrderNo()));

            BigDecimal orderRevenue = BigDecimal.ZERO;
            for (OrderItem item : items) {
                int num = item.getProductNum() != null ? item.getProductNum() : 0;
                BigDecimal amt = item.getProductAmount() != null ? item.getProductAmount() : BigDecimal.ZERO;
                salesCount7d += num;
                revenue7d = revenue7d.add(amt);
                orderRevenue = orderRevenue.add(amt);

                if (item.getProductId() != null) {
                    Long productId = item.getProductId();
                    productSales.put(productId, productSales.getOrDefault(productId, 0) + num);
                }
            }

            if (o.getCreateTime() != null) {
                if (!o.getCreateTime().isBefore(todayStart) && o.getCreateTime().isBefore(todayStart.plusDays(1))) {
                    todayOrders++;
                    todaySales = todaySales.add(orderRevenue);
                } else if (!o.getCreateTime().isBefore(yesterdayStart) && o.getCreateTime().isBefore(yesterdayStart.plusDays(1))) {
                    yesterdayOrders++;
                    yesterdaySales = yesterdaySales.add(orderRevenue);
                }
            }

            if (dayVO != null) {
                dayVO.setSales(dayVO.getSales().add(orderRevenue));
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

        vo.setTodayOrders(todayOrders);
        vo.setTodaySales(todaySales);
        vo.setYesterdayOrders(yesterdayOrders);
        vo.setYesterdaySales(yesterdaySales);

        vo.setPendingOrders(pendingOrders);
        vo.setPendingAfterSales(pendingAfterSales);
        vo.setLowStock(lowStock);

        List<DailySalesVO> salesTrend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start7d.plusDays(i);
            salesTrend.add(dailyMap.get(d));
        }
        vo.setSalesTrend(salesTrend);

        return vo;
    }

    public com.agricultural.assistplatform.vo.merchant.MerchantStatisticsVO statistics(String timeRange) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");

        // timeRange: today / week / month / year
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate prevStart;
        LocalDate prevEnd;
        LocalDate end = today;

        if ("today".equalsIgnoreCase(timeRange)) {
            start = today;
            prevStart = today.minusDays(1);
            prevEnd = today.minusDays(1);
        } else if ("month".equalsIgnoreCase(timeRange)) {
            start = today.minusDays(29);
            prevStart = today.minusDays(59);
            prevEnd = today.minusDays(30);
        } else if ("year".equalsIgnoreCase(timeRange)) {
            // year 聚合为近 12 个月（按月），start 为 11 个月前的第一天
            start = today.withDayOfMonth(1).minusMonths(11);
            prevStart = start.minusYears(1);
            prevEnd = start.minusDays(1);
        } else {
            // default: week
            start = today.minusDays(6);
            prevStart = today.minusDays(13);
            prevEnd = today.minusDays(7);
        }

        // 当前周期：订单（待发货/待收货/已完成），售后/取消不计入销售
        List<Integer> saleStatuses = Arrays.asList(2, 3, 4);
        LambdaQueryWrapper<OrderMain> baseQ = new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getMerchantId, merchantId)
                .in(OrderMain::getOrderStatus, saleStatuses);

        // 当前周期订单范围
        LocalDateTime startTime;
        LocalDateTime endTime;
        if ("year".equalsIgnoreCase(timeRange)) {
            startTime = start.atStartOfDay();
            endTime = today.plusDays(1).atStartOfDay();
        } else {
            startTime = start.atStartOfDay();
            endTime = end.plusDays(1).atStartOfDay();
        }

        List<OrderMain> orders = orderMainMapper.selectList(baseQ.ge(OrderMain::getCreateTime, startTime).lt(OrderMain::getCreateTime, endTime));

        int ordersCount = orders.size();
        BigDecimal salesAmount = BigDecimal.ZERO;

        // 订单号 -> 用于聚合明细
        Map<String, List<OrderItem>> orderItemsMap = new HashMap<>();
        if (!orders.isEmpty()) {
            List<String> orderNos = orders.stream().map(OrderMain::getOrderNo).collect(Collectors.toList());
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderNo, orderNos));
            for (OrderItem item : items) {
                orderItemsMap.computeIfAbsent(item.getOrderNo(), k -> new ArrayList<>()).add(item);
                salesAmount = salesAmount.add(item.getProductAmount() != null ? item.getProductAmount() : BigDecimal.ZERO);
            }
        }

        // 计算 topProducts
        Map<Long, Integer> productSales = new HashMap<>();
        for (OrderMain o : orders) {
            List<OrderItem> items = orderItemsMap.getOrDefault(o.getOrderNo(), Collections.emptyList());
            for (OrderItem item : items) {
                Long pid = item.getProductId();
                int num = item.getProductNum() != null ? item.getProductNum() : 0;
                if (pid != null && num > 0) {
                    productSales.put(pid, productSales.getOrDefault(pid, 0) + num);
                }
            }
        }

        List<HotProductVO> topProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    ProductInfo p = productInfoMapper.selectById(e.getKey());
                    HotProductVO v = new HotProductVO();
                    v.setProductId(e.getKey());
                    v.setProductName(p != null ? p.getProductName() : "");
                    v.setSalesVolume(e.getValue());
                    return v;
                })
                .collect(Collectors.toList());

        // 计算 salesTrendData（按日或按月）
        List<com.agricultural.assistplatform.vo.merchant.DailySalesVO> salesTrendData = new ArrayList<>();
        if ("year".equalsIgnoreCase(timeRange)) {
            // 12个月聚合
            LocalDate cursor = start;
            for (int i = 0; i < 12; i++) {
                LocalDate monthStart = cursor.plusMonths(i).withDayOfMonth(1);
                LocalDate monthEndExclusive = monthStart.plusMonths(1);
                BigDecimal monthSales = BigDecimal.ZERO;
                int monthOrders = 0;
                String monthLabel = String.format("%02d", monthStart.getMonthValue());
                // 订单聚合（简单做法：直接遍历过滤；数据量小适用）
                for (OrderMain o : orders) {
                    if (o.getCreateTime() == null) continue;
                    LocalDateTime ct = o.getCreateTime();
                    if (!ct.toLocalDate().isBefore(monthStart) && ct.toLocalDate().isBefore(monthEndExclusive)) {
                        monthOrders++;
                        List<OrderItem> items = orderItemsMap.getOrDefault(o.getOrderNo(), Collections.emptyList());
                        for (OrderItem item : items) {
                            monthSales = monthSales.add(item.getProductAmount() != null ? item.getProductAmount() : BigDecimal.ZERO);
                        }
                    }
                }
                com.agricultural.assistplatform.vo.merchant.DailySalesVO v = new com.agricultural.assistplatform.vo.merchant.DailySalesVO();
                v.setDate(monthLabel);
                v.setOrderCount(monthOrders);
                v.setSales(monthSales);
                salesTrendData.add(v);
            }
        } else {
            // 按日聚合
            Map<LocalDate, com.agricultural.assistplatform.vo.merchant.DailySalesVO> daily = new HashMap<>();
            for (int i = 0; i <= (int) (end.toEpochDay() - start.toEpochDay()); i++) {
                LocalDate d = start.plusDays(i);
                com.agricultural.assistplatform.vo.merchant.DailySalesVO v = new com.agricultural.assistplatform.vo.merchant.DailySalesVO();
                v.setDate(d.format(DateTimeFormatter.ofPattern("MM-dd")));
                v.setOrderCount(0);
                v.setSales(BigDecimal.ZERO);
                daily.put(d, v);
            }
            for (OrderMain o : orders) {
                if (o.getCreateTime() == null) continue;
                LocalDate d = o.getCreateTime().toLocalDate();
                com.agricultural.assistplatform.vo.merchant.DailySalesVO v = daily.get(d);
                if (v == null) continue;
                v.setOrderCount(v.getOrderCount() + 1);
                List<OrderItem> items = orderItemsMap.getOrDefault(o.getOrderNo(), Collections.emptyList());
                BigDecimal ds = v.getSales();
                BigDecimal add = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    add = add.add(item.getProductAmount() != null ? item.getProductAmount() : BigDecimal.ZERO);
                }
                v.setSales(ds.add(add));
            }
            // 按时间顺序输出，避免 MM-dd 字符串字典序导致错位
            for (int i = 0; i <= (int) (end.toEpochDay() - start.toEpochDay()); i++) {
                LocalDate d = start.plusDays(i);
                com.agricultural.assistplatform.vo.merchant.DailySalesVO v = daily.get(d);
                if (v != null) salesTrendData.add(v);
            }
        }

        // 订单状态分布（含 1-6）
        List<OrderMain> allOrdersInRange = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMain>()
                        .eq(OrderMain::getMerchantId, merchantId)
                        .ge(OrderMain::getCreateTime, startTime)
                        .lt(OrderMain::getCreateTime, endTime));
        int totalForDist = allOrdersInRange.size();
        Map<Integer, Long> countByStatus = allOrdersInRange.stream()
                .collect(Collectors.groupingBy(OrderMain::getOrderStatus, Collectors.counting()));

        List<com.agricultural.assistplatform.vo.merchant.OrderStatusStatVO> distribution = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : countByStatus.entrySet()) {
            int status = e.getKey();
            long cnt = e.getValue() != null ? e.getValue() : 0L;
            if (cnt <= 0) continue;
            double pct = totalForDist > 0 ? (cnt * 100.0 / totalForDist) : 0.0;
            com.agricultural.assistplatform.vo.merchant.OrderStatusStatVO s = new com.agricultural.assistplatform.vo.merchant.OrderStatusStatVO();
            s.setCount((int) cnt);
            s.setPercentage(pct);
            s.setName(getStatusName(status));
            s.setColor(getStatusColor(status));
            distribution.add(s);
        }

        // 与上一周期对比趋势
        // 上一周期订单：同样在销售状态中计算 sales/orders；对 visitors 采用同样 heuristics
        LocalDateTime prevStartTime;
        LocalDateTime prevEndTime;
        if ("year".equalsIgnoreCase(timeRange)) {
            prevStartTime = prevStart.atStartOfDay();
            prevEndTime = prevEnd.plusDays(1).atStartOfDay();
        } else {
            prevStartTime = prevStart.atStartOfDay();
            prevEndTime = prevEnd.plusDays(1).atStartOfDay();
        }
        List<OrderMain> prevOrders = orderMainMapper.selectList(
                baseQ.ge(OrderMain::getCreateTime, prevStartTime).lt(OrderMain::getCreateTime, prevEndTime));

        int prevOrdersCount = prevOrders.size();
        BigDecimal prevSalesAmount = BigDecimal.ZERO;
        if (!prevOrders.isEmpty()) {
            List<String> prevOrderNos = prevOrders.stream().map(OrderMain::getOrderNo).collect(Collectors.toList());
            List<OrderItem> prevItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderNo, prevOrderNos));
            for (OrderItem item : prevItems) {
                prevSalesAmount = prevSalesAmount.add(item.getProductAmount() != null ? item.getProductAmount() : BigDecimal.ZERO);
            }
        }

        // visitors / conversionRate：无法从当前表直接得到，使用订单强相关的可解释估算
        int completedCount = (int) orders.stream().filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 4).count();
        int prevCompletedCount = (int) prevOrders.stream().filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 4).count();
        int visitors = ordersCount * 10 + completedCount * 50;
        int prevVisitors = prevOrdersCount * 10 + prevCompletedCount * 50;

        BigDecimal conversionRate = visitors > 0 ? BigDecimal.valueOf(ordersCount * 1.0 / visitors * 100.0) : BigDecimal.ZERO;
        BigDecimal conversionTrend = percentageTrend(conversionRate, prevVisitors > 0
                ? BigDecimal.valueOf(prevOrdersCount * 1.0 / prevVisitors * 100.0)
                : BigDecimal.ZERO);

        // trend（百分比）
        BigDecimal salesTrend = percentageTrend(salesAmount, prevSalesAmount);
        BigDecimal ordersTrend = percentageTrend(BigDecimal.valueOf(ordersCount), BigDecimal.valueOf(prevOrdersCount));
        BigDecimal visitorsTrend = percentageTrend(BigDecimal.valueOf(visitors), BigDecimal.valueOf(prevVisitors));

        com.agricultural.assistplatform.vo.merchant.MerchantStatisticsVO vo = new com.agricultural.assistplatform.vo.merchant.MerchantStatisticsVO();
        vo.setSales(salesAmount);
        vo.setSalesTrend(salesTrend);
        vo.setOrders(ordersCount);
        vo.setOrdersTrend(ordersTrend);
        vo.setVisitors(visitors);
        vo.setVisitorsTrend(visitorsTrend);
        vo.setConversionRate(conversionRate);
        vo.setConversionTrend(conversionTrend);

        vo.setSalesTrendData(salesTrendData);
        vo.setTopProducts(topProducts);
        vo.setOrderStatusDistribution(distribution);

        return vo;
    }

    private String getStatusName(int status) {
        return switch (status) {
            case 1 -> "待付款";
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "支付异常";
            case 7 -> "售后中";
            case 8 -> "已完成售后";
            default -> "未知";
        };
    }

    private String getStatusColor(int status) {
        return switch (status) {
            case 1 -> "#f56c6c"; // 待付款
            case 2 -> "#409eff"; // 待发货
            case 3 -> "#67c23a"; // 待收货
            case 4 -> "#e6a23c"; // 已完成
            case 5 -> "#909399"; // 已取消
            case 6 -> "#f56c6c"; // 异常
            case 7 -> "#e6a23c"; // 售后中
            case 8 -> "#909399"; // 已完成售后
            default -> "#909399";
        };
    }

    /**
     * 返回趋势百分比：当前与上一期差值/上一期 * 100
     */
    private BigDecimal percentageTrend(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            // 上期为 0：如果本期也为 0，趋势为 0；否则给一个正的 100
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(100.0);
        }
        BigDecimal cur = current == null ? BigDecimal.ZERO : current;
        BigDecimal p = prev;
        return cur.subtract(p)
                .divide(p, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100.0));
    }

    public com.agricultural.assistplatform.common.PageResult<com.agricultural.assistplatform.entity.ReconciliationDetail> reconciliation(
            Integer pageNum,
            Integer pageSize,
            Integer transferStatus,
            String keyword,
            String startDate,
            String endDate) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<com.agricultural.assistplatform.entity.ReconciliationDetail> query = new LambdaQueryWrapper<com.agricultural.assistplatform.entity.ReconciliationDetail>()
                .eq(com.agricultural.assistplatform.entity.ReconciliationDetail::getMerchantId, merchantId);
        if (transferStatus != null) {
            query.eq(com.agricultural.assistplatform.entity.ReconciliationDetail::getTransferStatus, transferStatus);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(com.agricultural.assistplatform.entity.ReconciliationDetail::getOrderNo, keyword.trim());
        }
        if (start != null) {
            query.ge(com.agricultural.assistplatform.entity.ReconciliationDetail::getCreateTime, start.atStartOfDay());
        }
        if (end != null) {
            query.lt(com.agricultural.assistplatform.entity.ReconciliationDetail::getCreateTime, end.plusDays(1).atStartOfDay());
        }
        query.orderByDesc(com.agricultural.assistplatform.entity.ReconciliationDetail::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.agricultural.assistplatform.entity.ReconciliationDetail> page =
                reconciliationDetailMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                        query);
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    public com.agricultural.assistplatform.common.PageResult<com.agricultural.assistplatform.entity.SubsidyDetail> subsidy(
            Integer pageNum,
            Integer pageSize,
            Integer auditStatus,
            Integer grantStatus,
            String keyword,
            String startDate,
            String endDate) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        LambdaQueryWrapper<com.agricultural.assistplatform.entity.SubsidyDetail> query = new LambdaQueryWrapper<com.agricultural.assistplatform.entity.SubsidyDetail>()
                .eq(com.agricultural.assistplatform.entity.SubsidyDetail::getMerchantId, merchantId);
        if (auditStatus != null) {
            query.eq(com.agricultural.assistplatform.entity.SubsidyDetail::getAuditStatus, auditStatus);
        }
        if (grantStatus != null) {
            query.eq(com.agricultural.assistplatform.entity.SubsidyDetail::getGrantStatus, grantStatus);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(com.agricultural.assistplatform.entity.SubsidyDetail::getOrderNo, keyword.trim());
        }
        if (start != null) {
            query.ge(com.agricultural.assistplatform.entity.SubsidyDetail::getCreateTime, start.atStartOfDay());
        }
        if (end != null) {
            query.lt(com.agricultural.assistplatform.entity.SubsidyDetail::getCreateTime, end.plusDays(1).atStartOfDay());
        }
        query.orderByDesc(com.agricultural.assistplatform.entity.SubsidyDetail::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.agricultural.assistplatform.entity.SubsidyDetail> page =
                subsidyDetailMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                        query);
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    private LocalDate parseDate(String raw, String label) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "格式错误，应为yyyy-MM-dd");
        }
    }
}
