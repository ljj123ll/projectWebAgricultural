package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.AfterSaleFlow;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.UserMessageService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * 管理员售后裁决服务。
 * 负责后台售后列表查询，以及管理员对争议售后进行最终裁决和状态回写。
 */
public class AdminAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMainMapper orderMainMapper;
    private final UserMessageService userMessageService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final AdminAuditTrailService adminAuditTrailService;

    /**
     * 售后分页查询入口，支持按售后状态筛选。
     */
    public PageResult<AfterSale> list(Integer afterSaleStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<AfterSale>();
        if (afterSaleStatus != null) q.eq(AfterSale::getAfterSaleStatus, afterSaleStatus);
        q.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /**
     * 管理员裁决主流程。
     * 会校验当前售后是否允许裁决，并同步更新售后单、订单主状态、审计记录和消息通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Map<String, Object> body) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后单不存在");
        }
        if (!AfterSaleFlow.canAdminJudge(as.getAfterSaleStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前售后无需管理员裁决");
        }
        Integer targetStatus = body != null && body.get("afterSaleStatus") != null
                ? Integer.valueOf(String.valueOf(body.get("afterSaleStatus")))
                : as.getAfterSaleStatus();
        if (targetStatus == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "售后状态不能为空");
        }
        if (!AfterSaleFlow.isValidAdminTarget(targetStatus, as.getAfterSaleType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "裁决结果不合法，仅支持：完成售后 / 驳回申请 / 退货退款");
        }

        Long adminId = LoginContext.getUserId();
        if (adminId == null && body != null && body.get("adminId") != null) {
            adminId = Long.valueOf(String.valueOf(body.get("adminId")));
        }
        if (adminId != null) as.setAdminId(adminId);
        as.setAfterSaleStatus(targetStatus);

        String handleResult = body != null && body.get("handleResult") != null
                ? String.valueOf(body.get("handleResult"))
                : null;
        if (StringUtils.hasText(handleResult)) {
            as.setHandleResult(handleResult.trim());
        } else {
            as.setHandleResult(defaultHandleResult(targetStatus, as.getAfterSaleType()));
        }
        afterSaleMapper.updateById(as);
        adminAuditTrailService.record(
                "after_sale",
                id,
                targetStatus == AfterSaleFlow.STATUS_REJECTED ? 2 : 1,
                as.getHandleResult()
        );

        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, as.getOrderNo())
                .eq(OrderMain::getMerchantId, as.getMerchantId())
                .last("LIMIT 1"));
        if (order != null) {
            if (targetStatus == AfterSaleFlow.STATUS_RESOLVED) {
                order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_DONE);
            } else if (targetStatus == AfterSaleFlow.STATUS_REJECTED) {
                Integer restoreStatus = as.getOriginOrderStatus() == null ? 4 : as.getOriginOrderStatus();
                order.setOrderStatus(restoreStatus);
            } else {
                order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING);
            }
            orderMainMapper.updateById(order);
        }

        if (as.getUserId() != null) {
            switch (targetStatus) {
                case AfterSaleFlow.STATUS_RESOLVED -> userMessageService.createAdminMessage(
                        as.getUserId(),
                        "管理员裁决通过",
                        "管理员已裁决支持您的售后申请，当前售后已完成。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
                case AfterSaleFlow.STATUS_WAIT_USER_RETURN -> userMessageService.createAdminMessage(
                        as.getUserId(),
                        "管理员支持退货退款",
                        "管理员已裁决支持退货退款，请尽快上传退货快递单号。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
                case AfterSaleFlow.STATUS_REJECTED -> userMessageService.createAdminMessage(
                        as.getUserId(),
                        "管理员裁决未通过",
                        "管理员审核后未支持您的售后申请，可继续与商家沟通。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
                default -> userMessageService.createAdminMessage(
                        as.getUserId(),
                        "管理员已介入处理",
                        "管理员已更新您的售后处理进度，请进入售后详情查看。",
                        "after_sale",
                        as.getAfterSaleNo()
                );
            }

            userRealtimeEventService.publishRefresh(as.getUserId(), "AFTER_SALE_ADMIN_HANDLED", as.getAfterSaleNo());
        }
        if (as.getMerchantId() != null) {
            merchantRealtimeEventService.publishRefresh(as.getMerchantId(), "AFTER_SALE_ADMIN_HANDLED", as.getAfterSaleNo());
        }
        adminRealtimeEventService.publishRefreshToAll("AFTER_SALE_ADMIN_HANDLED", as.getAfterSaleNo());
    }

    /**
     * 生成不同裁决结果下的默认说明文案。
     */
    private String defaultHandleResult(Integer status, Integer afterSaleType) {
        if (status == null) return "管理员已更新售后处理状态。";
        if (status == AfterSaleFlow.STATUS_RESOLVED) return "管理员裁决：支持用户诉求，售后处理完成。";
        if (status == AfterSaleFlow.STATUS_WAIT_USER_RETURN) {
            if (afterSaleType != null && afterSaleType == 2) {
                return "管理员裁决：支持退货退款，请用户上传退货快递单号。";
            }
            return "管理员裁决：支持用户退款，售后处理中。";
        }
        if (status == AfterSaleFlow.STATUS_REJECTED) return "管理员裁决：当前售后申请未通过。";
        return "管理员已更新售后处理状态。";
    }
}
