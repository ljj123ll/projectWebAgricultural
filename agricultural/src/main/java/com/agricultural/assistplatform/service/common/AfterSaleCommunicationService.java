package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.AfterSaleCommunication;
import com.agricultural.assistplatform.mapper.AfterSaleCommunicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AfterSaleCommunicationService {

    private final AfterSaleCommunicationMapper afterSaleCommunicationMapper;

    public PageResult<AfterSaleCommunication> list(String afterSaleNo, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AfterSaleCommunication> q = new LambdaQueryWrapper<AfterSaleCommunication>()
                .eq(AfterSaleCommunication::getAfterSaleNo, afterSaleNo)
                .orderByDesc(AfterSaleCommunication::getCreateTime);
        Page<AfterSaleCommunication> page = afterSaleCommunicationMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void send(String afterSaleNo, Integer senderType, Long senderId, String content) {
        AfterSaleCommunication msg = new AfterSaleCommunication();
        msg.setAfterSaleNo(afterSaleNo);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setIsRead(0);
        afterSaleCommunicationMapper.insert(msg);
    }

    /**
     * 获取消息列表（支持分页加载更多）
     */
    public List<AfterSaleCommunication> getMessages(String afterSaleNo, Long lastId) {
        LambdaQueryWrapper<AfterSaleCommunication> query = new LambdaQueryWrapper<AfterSaleCommunication>()
                .eq(AfterSaleCommunication::getAfterSaleNo, afterSaleNo)
                .orderByDesc(AfterSaleCommunication::getCreateTime);
        
        // 如果提供了lastId，则加载更早的消息
        if (lastId != null) {
            query.lt(AfterSaleCommunication::getId, lastId);
        }
        
        // 限制返回20条
        query.last("LIMIT 20");
        
        return afterSaleCommunicationMapper.selectList(query);
    }

    /**
     * 发送消息
     */
    public void sendMessage(String afterSaleNo, Integer senderType, Long senderId, String content) {
        AfterSaleCommunication msg = new AfterSaleCommunication();
        msg.setAfterSaleNo(afterSaleNo);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setIsRead(0);
        afterSaleCommunicationMapper.insert(msg);
    }

    /**
     * 获取未读消息数
     * @param afterSaleNo 售后单号
     * @param senderType 发送者类型（查询对方发送的未读消息）
     *                   1-用户, 2-商家, 3-管理员
     *                   如果我是用户(senderType=1)，则查询商家和管理员发送的未读消息
     */
    public int getUnreadCount(String afterSaleNo, Integer senderType) {
        LambdaQueryWrapper<AfterSaleCommunication> query = new LambdaQueryWrapper<AfterSaleCommunication>()
                .eq(AfterSaleCommunication::getAfterSaleNo, afterSaleNo)
                .eq(AfterSaleCommunication::getIsRead, 0);
        
        // 根据senderType查询对方发送的消息
        // 如果senderType是1(用户)，则查询商家(2)和管理员(3)发送的消息
        // 如果senderType是2(商家)，则查询用户(1)和管理员(3)发送的消息
        if (senderType != null) {
            query.ne(AfterSaleCommunication::getSenderType, senderType);
        }
        
        return Math.toIntExact(afterSaleCommunicationMapper.selectCount(query));
    }

    /**
     * 标记消息为已读
     */
    public void markAsRead(String afterSaleNo, Integer senderType) {
        LambdaUpdateWrapper<AfterSaleCommunication> update = new LambdaUpdateWrapper<AfterSaleCommunication>()
                .eq(AfterSaleCommunication::getAfterSaleNo, afterSaleNo)
                .eq(AfterSaleCommunication::getIsRead, 0);
        
        // 标记对方发送的消息为已读
        if (senderType != null) {
            update.ne(AfterSaleCommunication::getSenderType, senderType);
        }
        
        update.set(AfterSaleCommunication::getIsRead, 1);
        afterSaleCommunicationMapper.update(null, update);
    }
}
