package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.SubsidyDetail;
import com.agricultural.assistplatform.service.merchant.MerchantStatsService;
import com.agricultural.assistplatform.vo.merchant.MerchantStatsVO;
import com.agricultural.assistplatform.vo.merchant.MerchantStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端-数据统计")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantStatsController {

    private final MerchantStatsService merchantStatsService;

    @Operation(summary = "工作台统计数据")
    @GetMapping("/stats")
    public Result<MerchantStatsVO> stats() {
        return Result.ok(merchantStatsService.stats());
    }

    @Operation(summary = "统计页面数据")
    @GetMapping("/statistics")
    public Result<MerchantStatisticsVO> statistics(@RequestParam(defaultValue = "week") String timeRange) {
        return Result.ok(merchantStatsService.statistics(timeRange));
    }

    @Operation(summary = "对账明细")
    @GetMapping("/reconciliation")
    public Result<PageResult<ReconciliationDetail>> reconciliation(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer transferStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(merchantStatsService.reconciliation(pageNum, pageSize, transferStatus, keyword, startDate, endDate));
    }

    @Operation(summary = "补贴明细")
    @GetMapping("/subsidy")
    public Result<PageResult<SubsidyDetail>> subsidy(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer grantStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(merchantStatsService.subsidy(pageNum, pageSize, auditStatus, grantStatus, keyword, startDate, endDate));
    }
}
