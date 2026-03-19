package com.agricultural.assistplatform.controller.common;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.LogisticsInfo;
import com.agricultural.assistplatform.service.common.LogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common/logistics")
@Tag(name = "物流信息模块")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/{orderNo}")
    @Operation(summary = "获取物流信息")
    public Result<LogisticsInfo> getLogistics(@PathVariable String orderNo) {
        LogisticsInfo logistics = logisticsService.getByOrderNo(orderNo);
        return Result.ok(logistics);
    }
}
