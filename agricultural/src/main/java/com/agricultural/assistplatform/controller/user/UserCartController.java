package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserCartService;
import com.agricultural.assistplatform.vo.user.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户端-购物车")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserCartController {

    private final UserCartService userCartService;

    @Operation(summary = "查看购物车")
    @GetMapping("/cart")
    public Result<List<CartItemVO>> list() {
        return Result.ok(userCartService.list());
    }

    @Operation(summary = "添加商品")
    @PostMapping("/cart")
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        Integer num = body.get("productNum") != null ? Integer.valueOf(body.get("productNum").toString()) : 1;
        if (productId == null) return Result.fail(400, "商品ID不能为空");
        userCartService.add(productId, num);
        return Result.ok();
    }

    @Operation(summary = "修改数量")
    @PutMapping("/cart/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer num = body != null ? body.get("productNum") : null;
        userCartService.updateNum(id, num);
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/cart/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userCartService.delete(id);
        return Result.ok();
    }
}
