package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.mapper.LogisticsInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsInfoMapper logisticsInfoMapper;

    public LogisticsInfo getByOrderNo(String orderNo) {
        return logisticsInfoMapper.selectOne(
                new LambdaQueryWrapper<LogisticsInfo>()
                        .eq(LogisticsInfo::getOrderNo, orderNo)
        );
    }
}
