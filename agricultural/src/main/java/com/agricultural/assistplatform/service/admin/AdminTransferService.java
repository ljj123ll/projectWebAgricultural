package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.mapper.ReconciliationDetailMapper;
import com.agricultural.assistplatform.mapper.LogisticsInfoMapper;
import com.agricultural.assistplatform.vo.admin.RiskOrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTransferService {

    private final ReconciliationDetailMapper reconciliationDetailMapper;
    private final OrderMainMapper orderMainMapper;
    private final LogisticsInfoMapper logisticsInfoMapper;

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
        List<LogisticsInfo> timeoutLogistics = logisticsInfoMapper.selectList(new LambdaQueryWrapper<LogisticsInfo>()
                .eq(LogisticsInfo::getLogisticsStatus, 1)
                .lt(LogisticsInfo::getUpdateTime, deadline)
                .orderByAsc(LogisticsInfo::getUpdateTime));
        if (timeoutLogistics.isEmpty()) {
            return com.agricultural.assistplatform.common.PageResult.of(0L, List.of());
        }
        Set<String> orderNos = timeoutLogistics.stream()
                .map(LogisticsInfo::getOrderNo)
                .filter(no -> no != null && !no.isBlank())
                .collect(Collectors.toSet());

        if (orderNos.isEmpty()) {
            return com.agricultural.assistplatform.common.PageResult.of(0L, List.of());
        }

        Page<OrderMain> page = orderMainMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<OrderMain>()
                        .eq(OrderMain::getOrderStatus, 3)
                        .in(OrderMain::getOrderNo, orderNos)
                        .orderByAsc(OrderMain::getUpdateTime));
        // 给没有异常原因的物流记录补齐默认提示，便于管理端直接识别
        for (LogisticsInfo logisticsInfo : timeoutLogistics) {
            if (logisticsInfo.getAbnormalReason() == null || logisticsInfo.getAbnormalReason().isBlank()) {
                logisticsInfo.setAbnormalReason("物流超时超过5天，请核实并处理");
                logisticsInfoMapper.updateById(logisticsInfo);
            }
        }
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), page.getRecords());
    }

    public com.agricultural.assistplatform.common.PageResult<RiskOrderVO> riskOrders(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LocalDateTime windowStart = LocalDateTime.now().minusDays(3);
        List<OrderMain> sourceOrders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .ge(OrderMain::getCreateTime, windowStart)
                .in(OrderMain::getOrderStatus, 2, 3, 4, 7, 8)
                .orderByDesc(OrderMain::getCreateTime));

        if (sourceOrders.isEmpty()) {
            return com.agricultural.assistplatform.common.PageResult.of(0L, List.of());
        }

        Map<String, List<OrderMain>> byAddress = sourceOrders.stream()
                .collect(Collectors.groupingBy(it -> normalizeAddress(it.getReceiverAddress())));
        Map<String, List<OrderMain>> byUserMerchant = sourceOrders.stream()
                .collect(Collectors.groupingBy(it -> (it.getUserId() == null ? 0 : it.getUserId())
                        + "#" + (it.getMerchantId() == null ? 0 : it.getMerchantId())));

        List<RiskOrderVO> riskList = new ArrayList<>();
        for (OrderMain order : sourceOrders) {
            List<String> reasons = new ArrayList<>();
            int score = 0;

            String addressKey = normalizeAddress(order.getReceiverAddress());
            List<OrderMain> sameAddressOrders = byAddress.getOrDefault(addressKey, List.of());
            BigDecimal sameAddressAmount = sameAddressOrders.stream()
                    .map(OrderMain::getTotalAmount)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sameAddressOrders.size() >= 3) {
                reasons.add("同一地址短期多单（" + sameAddressOrders.size() + "笔）");
                score += 45;
            }
            if (sameAddressAmount.compareTo(BigDecimal.valueOf(2000)) >= 0) {
                reasons.add("同一地址短期累计大额（¥" + sameAddressAmount.setScale(2, RoundingMode.HALF_UP) + "）");
                score += 35;
            }

            String pairKey = (order.getUserId() == null ? 0 : order.getUserId())
                    + "#" + (order.getMerchantId() == null ? 0 : order.getMerchantId());
            int pairCount = byUserMerchant.getOrDefault(pairKey, List.of()).size();
            if (pairCount >= 5) {
                reasons.add("同一用户与商家短期高频交易（" + pairCount + "笔）");
                score += 30;
            }

            if (reasons.isEmpty()) continue;
            RiskOrderVO vo = new RiskOrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setMerchantId(order.getMerchantId());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setReceiver(order.getReceiver());
            vo.setReceiverPhone(order.getReceiverPhone());
            vo.setReceiverAddress(order.getReceiverAddress());
            vo.setRiskReason(String.join("；", reasons));
            vo.setRiskScore(Math.min(score, 100));
            vo.setCreateTime(order.getCreateTime());
            riskList.add(vo);
        }

        riskList.sort(Comparator.comparing(RiskOrderVO::getRiskScore).reversed()
                .thenComparing(RiskOrderVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        int total = riskList.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return com.agricultural.assistplatform.common.PageResult.of((long) total, riskList.subList(from, to));
    }

    private String normalizeAddress(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\s+", "")
                .replace("，", "")
                .replace(",", "")
                .replace("。", "")
                .toLowerCase(Locale.ROOT);
    }
}
