package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.OrderCommunication;
import com.agricultural.assistplatform.entity.OrderMain;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.OrderCommunicationMapper;
import com.agricultural.assistplatform.mapper.OrderMainMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderCommunicationService {

    private static final int SENDER_USER = 1;
    private static final int SENDER_MERCHANT = 2;
    private static final int SENDER_ADMIN = 3;

    private static final int MESSAGE_TEXT = 1;
    private static final int MESSAGE_IMAGE = 2;
    private static final int MESSAGE_VIDEO = 3;

    private final OrderCommunicationMapper orderCommunicationMapper;
    private final OrderMainMapper orderMainMapper;

    public PageResult<OrderCommunication> list(String orderNo, Long actorId, String loginType, Integer pageNum, Integer pageSize) {
        ensureAccess(orderNo, actorId, loginType);
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        LambdaQueryWrapper<OrderCommunication> query = new LambdaQueryWrapper<OrderCommunication>()
                .eq(OrderCommunication::getOrderNo, orderNo)
                .orderByAsc(OrderCommunication::getId);
        Page<OrderCommunication> page = orderCommunicationMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public OrderCommunication send(String orderNo,
                                   Long actorId,
                                   String loginType,
                                   String content,
                                   Integer messageType,
                                   String mediaUrl,
                                   String mediaName) {
        ensureAccess(orderNo, actorId, loginType);

        int type = (messageType == null || messageType <= 0) ? MESSAGE_TEXT : messageType;
        if (type != MESSAGE_TEXT && type != MESSAGE_IMAGE && type != MESSAGE_VIDEO) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的消息类型");
        }

        String safeContent = content == null ? "" : content.trim();
        String safeMediaUrl = mediaUrl == null ? "" : mediaUrl.trim();
        String safeMediaName = mediaName == null ? "" : mediaName.trim();

        if (type == MESSAGE_TEXT) {
            if (!StringUtils.hasText(safeContent)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "文本消息不能为空");
            }
        } else {
            if (!StringUtils.hasText(safeMediaUrl)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "媒体消息缺少文件地址");
            }
            if (!safeMediaUrl.startsWith("/uploads/")) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "媒体地址非法");
            }
        }

        if (safeContent.length() > 500) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能超过500字");
        }
        if (safeMediaUrl.length() > 255) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "媒体地址过长");
        }
        if (safeMediaName.length() > 255) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "媒体名称过长");
        }

        OrderCommunication message = new OrderCommunication();
        message.setOrderNo(orderNo);
        message.setSenderType(resolveSenderType(loginType));
        message.setSenderId(actorId);
        message.setMessageType(type);
        message.setContent(safeContent);
        message.setMediaUrl(StringUtils.hasText(safeMediaUrl) ? safeMediaUrl : null);
        message.setMediaName(StringUtils.hasText(safeMediaName) ? safeMediaName : null);
        orderCommunicationMapper.insert(message);
        return message;
    }

    public boolean canAccessOrder(String orderNo, Long actorId, String loginType) {
        if (!StringUtils.hasText(orderNo) || actorId == null || !StringUtils.hasText(loginType)) {
            return false;
        }
        OrderMain order = findOrder(orderNo);
        if (order == null) return false;
        return switch (loginType) {
            case "user" -> actorId.equals(order.getUserId());
            case "merchant" -> actorId.equals(order.getMerchantId());
            case "admin" -> true;
            default -> false;
        };
    }

    private void ensureAccess(String orderNo, Long actorId, String loginType) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单号不能为空");
        }
        if (actorId == null || !StringUtils.hasText(loginType)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        OrderMain order = findOrder(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        boolean ok = switch (loginType) {
            case "user" -> actorId.equals(order.getUserId());
            case "merchant" -> actorId.equals(order.getMerchantId());
            case "admin" -> true;
            default -> false;
        };
        if (!ok) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该订单沟通");
        }
    }

    private int resolveSenderType(String loginType) {
        return switch (loginType) {
            case "user" -> SENDER_USER;
            case "merchant" -> SENDER_MERCHANT;
            case "admin" -> SENDER_ADMIN;
            default -> throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        };
    }

    private OrderMain findOrder(String orderNo) {
        return orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, orderNo)
                .last("LIMIT 1"));
    }
}
