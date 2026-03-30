package com.agricultural.assistplatform.service.user;

import cn.hutool.core.util.IdUtil;
import com.agricultural.assistplatform.common.AfterSaleFlow;
import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.user.AfterSaleApplyDTO;
import com.agricultural.assistplatform.dto.user.AfterSaleReturnLogisticsDTO;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.agricultural.assistplatform.service.common.MerchantRealtimeEventService;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.UserMessageService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 用户端售后服务。
 * 负责售后申请、管理员介入申请、退货物流提交以及售后详情查询。
 */
public class UserAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMainMapper orderMainMapper;
    private final MerchantRealtimeEventService merchantRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final UserMessageService userMessageService;

    /**
     * 用户提交售后申请。
     * 会校验订单状态、拦截重复售后，并把订单推进到“售后中”状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleVO apply(AfterSaleApplyDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, dto.getOrderNo()).eq(OrderMain::getUserId, userId));
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (order.getOrderStatus() != 3 && order.getOrderStatus() != 4)
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅待收货/已完成订单可申请售后");

        Long activeCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderNo, dto.getOrderNo())
                .in(AfterSale::getAfterSaleStatus,
                        AfterSaleFlow.STATUS_PENDING_MERCHANT,
                        AfterSaleFlow.STATUS_WAIT_MERCHANT_REFUND,
                        AfterSaleFlow.STATUS_ADMIN,
                        AfterSaleFlow.STATUS_WAIT_USER_RETURN));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该订单存在处理中售后，请勿重复申请");
        }

        AfterSale as = new AfterSale();
        as.setAfterSaleNo("AS" + IdUtil.getSnowflakeNextIdStr());
        as.setOrderNo(dto.getOrderNo());
        as.setUserId(userId);
        as.setMerchantId(order.getMerchantId());
        as.setAfterSaleType(dto.getAfterSaleType());
        as.setApplyReason(dto.getApplyReason());
        as.setProofImgUrls(dto.getProofImgUrls());
        as.setAfterSaleStatus(AfterSaleFlow.STATUS_PENDING_MERCHANT);
        as.setOriginOrderStatus(order.getOrderStatus());
        afterSaleMapper.insert(as);

        order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING);
        orderMainMapper.updateById(order);

        merchantRealtimeEventService.publishRefresh(order.getMerchantId(), "AFTER_SALE_CREATED", as.getAfterSaleNo());
        userRealtimeEventService.publishRefresh(userId, "AFTER_SALE_CREATED", as.getAfterSaleNo());
        adminRealtimeEventService.publishRefreshToAll("AFTER_SALE_CREATED", as.getAfterSaleNo());
        userMessageService.createSystemMessage(
                userId,
                "售后申请已提交",
                "您的售后申请已提交，商家会尽快处理。",
                "after_sale",
                as.getAfterSaleNo()
        );
        return toVO(as);
    }

    /**
     * 用户售后列表查询。
     */
    public com.agricultural.assistplatform.common.PageResult<AfterSaleVO> list(Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getUserId, userId).orderByDesc(AfterSale::getCreateTime));
        List<AfterSaleVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return com.agricultural.assistplatform.common.PageResult.of(page.getTotal(), list);
    }

    /**
     * 用户申请管理员介入。
     * 当商家处理结果存在争议时，会把售后单升级到平台裁决阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public void escalate(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id).eq(AfterSale::getUserId, userId));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后不存在");
        if (!AfterSaleFlow.canUserEscalate(as.getAfterSaleStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前阶段不支持申请管理员介入");
        }
        as.setAfterSaleStatus(AfterSaleFlow.STATUS_ADMIN);
        if (as.getHandleResult() == null || as.getHandleResult().isBlank()) {
            as.setHandleResult("用户已申请管理员介入，请等待平台裁决。");
        }
        afterSaleMapper.updateById(as);

        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, as.getOrderNo())
                .eq(OrderMain::getUserId, userId)
                .last("LIMIT 1"));
        if (order != null && !isOrderAfterSaleProcessing(order.getOrderStatus())) {
            order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING);
            orderMainMapper.updateById(order);
        }

        merchantRealtimeEventService.publishRefresh(as.getMerchantId(), "AFTER_SALE_ESCALATED", as.getAfterSaleNo());
        userRealtimeEventService.publishRefresh(userId, "AFTER_SALE_ESCALATED", as.getAfterSaleNo());
        adminRealtimeEventService.publishRefreshToAll("AFTER_SALE_ESCALATED", as.getAfterSaleNo());
        userMessageService.createSystemMessage(
                userId,
                "已申请管理员介入",
                "平台已介入处理，请在售后详情中关注裁决结果。",
                "after_sale",
                as.getAfterSaleNo()
        );
    }

    /**
     * 用户提交退货物流。
     * 适用于“退货退款”型售后，提交后状态流转为待商家收货退款。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitReturnLogistics(Long id, AfterSaleReturnLogisticsDTO dto) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getId, id)
                .eq(AfterSale::getUserId, userId)
                .last("LIMIT 1"));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后不存在");
        if (as.getAfterSaleType() == null || as.getAfterSaleType() != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前售后类型无需填写退货物流");
        }
        if (as.getAfterSaleStatus() == null || as.getAfterSaleStatus() != AfterSaleFlow.STATUS_WAIT_USER_RETURN) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前阶段不可填写退货物流");
        }

        String logisticsNo = dto.getLogisticsNo() == null ? "" : dto.getLogisticsNo().trim();
        if (logisticsNo.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "退货物流单号不能为空");
        String logisticsCompany = dto.getLogisticsCompany() == null ? "" : dto.getLogisticsCompany().trim();
        if (logisticsCompany.isBlank()) logisticsCompany = "其他快递";

        as.setReturnLogisticsNo(logisticsNo);
        as.setReturnLogisticsCompany(logisticsCompany);
        as.setReturnShipTime(LocalDateTime.now());
        as.setAfterSaleStatus(AfterSaleFlow.STATUS_WAIT_MERCHANT_REFUND);
        as.setHandleResult("用户已寄回商品，待商家签收退款");
        afterSaleMapper.updateById(as);

        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, as.getOrderNo())
                .eq(OrderMain::getUserId, userId)
                .last("LIMIT 1"));
        if (order != null && !isOrderAfterSaleProcessing(order.getOrderStatus())) {
            order.setOrderStatus(AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING);
            orderMainMapper.updateById(order);
        }

        merchantRealtimeEventService.publishRefresh(as.getMerchantId(), "AFTER_SALE_RETURN_SHIPPED", as.getAfterSaleNo());
        userRealtimeEventService.publishRefresh(userId, "AFTER_SALE_RETURN_SHIPPED", as.getAfterSaleNo());
        adminRealtimeEventService.publishRefreshToAll("AFTER_SALE_RETURN_SHIPPED", as.getAfterSaleNo());
        userMessageService.createSystemMessage(
                userId,
                "退货物流已提交",
                "已提交退货物流信息，请等待商家确认收货并退款。",
                "after_sale",
                as.getAfterSaleNo()
        );
    }

    /**
     * 按售后单号查看详情。
     */
    public AfterSaleVO getDetailByNo(String afterSaleNo) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        AfterSale as = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getAfterSaleNo, afterSaleNo)
                .eq(AfterSale::getUserId, userId));
        if (as == null) throw new BusinessException(ResultCode.NOT_FOUND, "售后不存在");
        return toVO(as);
    }

    private boolean isOrderAfterSaleProcessing(Integer orderStatus) {
        return orderStatus != null && orderStatus == AfterSaleFlow.ORDER_AFTER_SALE_PROCESSING;
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
