package com.agricultural.assistplatform.controller.merchant;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.ReconciliationDetail;
import com.agricultural.assistplatform.entity.SubsidyDetail;
import com.agricultural.assistplatform.service.merchant.MerchantStatsService;
import com.agricultural.assistplatform.vo.merchant.MerchantStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商家端-数据对账")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantStatsController {

    private final MerchantStatsService merchantStatsService;

    @Operation(summary = "核心数据（近7天销量/营收）")
    @GetMapping("/stats")
    public Result<MerchantStatsVO> stats() {
        return Result.ok(merchantStatsService.stats());
    }

    @Operation(summary = "对账明细")
    @GetMapping("/reconciliation")
    public Result<PageResult<ReconciliationDetail>> reconciliation(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(merchantStatsService.reconciliation(pageNum, pageSize));
    }

    @Operation(summary = "补贴明细")
    @GetMapping("/subsidy")
    public Result<PageResult<SubsidyDetail>> subsidy(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(merchantStatsService.subsidy(pageNum, pageSize));
    }
}
