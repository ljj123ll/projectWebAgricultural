package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.admin.AdminDashboardService;
import com.agricultural.assistplatform.vo.admin.DashboardVO;
import com.agricultural.assistplatform.vo.admin.MerchantRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员端-数据看板")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "平台核心数据")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(adminDashboardService.dashboard());
    }

    @Operation(summary = "商家销量排行")
    @GetMapping("/merchants/rank")
    public Result<List<MerchantRankVO>> rank(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(adminDashboardService.merchantRank(limit));
    }
}
