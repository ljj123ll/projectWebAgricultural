package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.AfterSaleFlow;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.UserMessageService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
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
public class MerchantAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMainMapper orderMainMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final UserMessageService userMessageService;

    public com.agricultural.assistplatform.common.PageResult<AfterSaleVO> list(Integer afterSaleStatus, Integer pageNum, Integer pageSize) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getMerchantId, merchantId);
        if (afterSaleStatus != null) q.eq(AfterSale::getAfterSaleStatus, afterSaleStatus);
        q.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), q);
        List<AfterSaleVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Map<String, Object> body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id).eq(AfterSale::getMerchantId, merchantId));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        if (!AfterSaleFlow.canMerchantHandle(as.getAfterSaleStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不可处理，仅待商家处理状态可同意或拒绝");
        }
        Boolean agree = parseAgree(body);
        if (agree == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请明确选择同意或拒绝");
        }
        String handleResult = normalizeRemark(body == null ? null : body.get("handleResult"));
        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, as.getOrderNo())
                .eq(OrderMain::getMerchantId, merchantId)
                .last("LIMIT 1"));

        if (Boolean.TRUE.equals(agree)) {
            if (as.getAfterSaleType() != null && as.getAfterSaleType() == 2) {
                // 退货退款：先等待用户填写退货物流
                as.setAfterSaleStatus(AfterSaleFlow.STATUS_WAIT_USER_RETURN);
                as.setHandleResult(handleResult == null
                        ? "商家已同意退货退款，请尽快填写退货快递单号。"
                        : handleResult);
                if (order != null) {
                    order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING);
                    orderMainMapper.updateById(order);
                }
                userMessageService.createMerchantMessage(
                        as.getUserId(),
                        "商家已同意退货退款",
                        "请尽快上传退货快递单号，商家签收后将完成退款。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
            } else {
                // 仅退款/换货等：直接完成售后
                as.setAfterSaleStatus(AfterSaleFlow.STATUS_RESOLVED);
                as.setHandleResult(handleResult == null ? "商家已同意退款，售后已完成。" : handleResult);
                if (order != null) {
                    order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_DONE);
                    orderMainMapper.updateById(order);
                }
                userMessageService.createMerchantMessage(
                        as.getUserId(),
                        "售后退款已完成",
                        "您的售后申请已处理完成，请注意查收退款。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
            }
        } else {
            as.setAfterSaleStatus(AfterSaleFlow.STATUS_REJECTED);
            as.setHandleResult(handleResult == null ? "商家已拒绝该售后申请。" : handleResult);
            if (order != null) {
                Integer restoreStatus = as.getOriginOrderStatus() == null ? 4 : as.getOriginOrderStatus();
                order.setOrderStatus(restoreStatus);
                orderMainMapper.updateById(order);
            }
            userMessageService.createMerchantMessage(
                    as.getUserId(),
                    "售后申请未通过",
                    as.getHandleResult(),
                    "after_sale",
                    as.getAfterSaleNo()
            );
        }
        afterSaleMapper.updateById(as);
        merchantRealtimeEventService.publishRefresh(merchantId, "AFTER_SALE_HANDLED", as.getAfterSaleNo());
        userRealtimeEventService.publishRefresh(as.getUserId(), "AFTER_SALE_HANDLED", as.getAfterSaleNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(Long id, Map<String, Object> body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id)
                .eq(AfterSale::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        if (as.getAfterSaleType() == null || as.getAfterSaleType() != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前售后类型无需确认退货");
        }
        if (!AfterSaleFlow.canMerchantConfirmReturn(as.getAfterSaleStatus(), as.getAfterSaleType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前阶段不可确认退货");
        }
        if (as.getReturnLogisticsNo() == null || as.getReturnLogisticsNo().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户尚未填写退货物流单号");
        }

        String handleResult = normalizeRemark(body == null ? null : body.get("handleResult"));
        as.setAfterSaleStatus(AfterSaleFlow.STATUS_RESOLVED);
        as.setHandleResult(handleResult == null ? "商家已确认收到退货，退款完成，售后已结束。" : handleResult);
        afterSaleMapper.updateById(as);

        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, as.getOrderNo())
                .eq(OrderMain::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (order != null) {
            order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_DONE);
            orderMainMapper.updateById(order);
        }

        userMessageService.createMerchantMessage(
                as.getUserId(),
                "退货已签收，退款完成",
                "商家已确认收到您的退货，退款已处理完成。",
                "after_sale",
                as.getAfterSaleNo()
        );
        merchantRealtimeEventService.publishRefresh(merchantId, "AFTER_SALE_RESOLVED", as.getAfterSaleNo());
        userRealtimeEventService.publishRefresh(as.getUserId(), "AFTER_SALE_RESOLVED", as.getAfterSaleNo());
    }

    private String normalizeRemark(Object rawRemark) {
        if (rawRemark == null) return null;
        String text = String.valueOf(rawRemark).trim();
        return text.isEmpty() ? null : text;
    }

    private Boolean parseAgree(Map<String, Object> body) {
        if (body == null || !body.containsKey("agree")) return null;
        Object raw = body.get("agree");
        if (raw instanceof Boolean bool) return bool;
        String text = String.valueOf(raw).trim();
        if ("true".equalsIgnoreCase(text) || "1".equals(text)) return true;
        if ("false".equalsIgnoreCase(text) || "0".equals(text)) return false;
        return null;
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
        vo.setOriginOrderStatus(a.getOriginOrderStatus());
        vo.setReturnLogisticsCompany(a.getReturnLogisticsCompany());
        vo.setReturnLogisticsNo(a.getReturnLogisticsNo());
        vo.setReturnShipTime(a.getReturnShipTime());
        vo.setHandleResult(a.getHandleResult());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
