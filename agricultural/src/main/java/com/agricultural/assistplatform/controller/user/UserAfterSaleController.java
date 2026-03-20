package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.dto.user.AfterSaleApplyDTO;
import com.agricultural.assistplatform.service.user.UserAfterSaleService;
import com.agricultural.assistplatform.vo.user.AfterSaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-售后")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAfterSaleController {

    private final UserAfterSaleService userAfterSaleService;

    @Operation(summary = "发起售后")
    @PostMapping("/after-sale")
    public Result<AfterSaleVO> apply(@Valid @RequestBody AfterSaleApplyDTO dto) {
        return Result.ok(userAfterSaleService.apply(dto));
    }

    @Operation(summary = "售后列表")
    @GetMapping("/after-sale")
    public Result<PageResult<AfterSaleVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userAfterSaleService.list(pageNum, pageSize));
    }

    @Operation(summary = "申请管理员介入")
    @PutMapping("/after-sale/{id}/escalate")
    public Result<Void> escalate(@PathVariable Long id) {
        userAfterSaleService.escalate(id);
        return Result.ok();
    }

    @Operation(summary = "售后详情（根据售后单号）")
    @GetMapping("/after-sale/{no}")
    public Result<AfterSaleVO> detail(@PathVariable("no") String no) {
        return Result.ok(userAfterSaleService.getDetailByNo(no));
    }
}
