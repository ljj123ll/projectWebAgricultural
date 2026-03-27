package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.annotation.AdminPermission;
import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.admin.AdminUnsalableService;
import com.agricultural.assistplatform.vo.common.UnsalableProductVO;
import com.agricultural.assistplatform.vo.common.UnsalableSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员端-滞销专区")
@RestController
@RequestMapping("/admin/unsalable")
@RequiredArgsConstructor
@AdminPermission("unsalable:manage")
public class AdminUnsalableController {

    private final AdminUnsalableService adminUnsalableService;

    @Operation(summary = "获取滞销商品列表")
    @GetMapping
    public Result<PageResult<UnsalableProductVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "all") String sourceType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminUnsalableService.list(keyword, sortBy, sourceType, pageNum, pageSize));
    }

    @Operation(summary = "获取滞销商品概览")
    @GetMapping("/summary")
    public Result<UnsalableSummaryVO> summary(@RequestParam(required = false) String keyword) {
        return Result.ok(adminUnsalableService.summary(keyword));
    }

    @Operation(summary = "设置/取消滞销商品")
    @PutMapping("/{id}")
    public Result<Void> setUnsalable(@PathVariable Long id, @RequestParam Integer isUnsalable) {
        adminUnsalableService.setUnsalable(id, isUnsalable);
        return Result.ok();
    }
}
