package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.AfterSaleCommunication;
import com.agricultural.assistplatform.mapper.AfterSaleCommunicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
