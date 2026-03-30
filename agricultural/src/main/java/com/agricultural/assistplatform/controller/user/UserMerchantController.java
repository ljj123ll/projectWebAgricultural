package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserMerchantService;
import com.agricultural.assistplatform.vo.user.MerchantCommentVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.agricultural.assistplatform.vo.user.UserMerchantShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-商家主页")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserMerchantController {

    private final UserMerchantService userMerchantService;

    @Operation(summary = "商家主页信息")
    @GetMapping("/merchants/{id}")
    public Result<UserMerchantShopVO> shop(@PathVariable Long id) {
        return Result.ok(userMerchantService.getShop(id));
    }

    @Operation(summary = "商家商品列表")
    @GetMapping("/merchants/{id}/products")
    public Result<PageResult<ProductSimpleVO>> products(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userMerchantService.products(id, pageNum, pageSize));
    }

    @Operation(summary = "商家评论汇总")
    @GetMapping("/merchants/{id}/comments")
    public Result<PageResult<MerchantCommentVO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userMerchantService.comments(id, pageNum, pageSize));
    }
}
