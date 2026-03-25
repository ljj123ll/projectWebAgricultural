package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.UserMessage;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.UserMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMessageService {

    public static final int SENDER_SYSTEM = 1;
    public static final int SENDER_MERCHANT = 2;
    public static final int SENDER_ADMIN = 3;

    private final UserMessageMapper userMessageMapper;

    public PageResult<UserMessage> list(Long userId, Integer senderType, Integer isRead, Integer pageNum, Integer pageSize) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LambdaQueryWrapper<UserMessage> q = new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId);
        if (senderType != null) q.eq(UserMessage::getSenderType, senderType);
        if (isRead != null) q.eq(UserMessage::getIsRead, isRead);
        q.orderByDesc(UserMessage::getCreateTime).orderByDesc(UserMessage::getId);

        Page<UserMessage> page = userMessageMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public long unreadCount(Long userId) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return userMessageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0));
    }

    public void markRead(Long userId, Long messageId) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        userMessageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getId, messageId)
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1));
    }

    public void markAllRead(Long userId) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        userMessageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1));
    }

    public void createSystemMessage(Long userId, String title, String content, String refType, String refNo) {
        createMessage(userId, SENDER_SYSTEM, title, content, refType, refNo);
    }

    public void createMerchantMessage(Long userId, String title, String content, String refType, String refNo) {
        createMessage(userId, SENDER_MERCHANT, title, content, refType, refNo);
    }

    public void createAdminMessage(Long userId, String title, String content, String refType, String refNo) {
        createMessage(userId, SENDER_ADMIN, title, content, refType, refNo);
    }

    public void createMessage(Long userId, Integer senderType, String title, String content, String refType, String refNo) {
        if (userId == null || senderType == null) return;
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setSenderType(senderType);
        message.setTitle(title);
        message.setContent(content);
        message.setRefType(refType);
        message.setRefNo(refNo);
        message.setIsRead(0);
        userMessageMapper.insert(message);
    }
}
