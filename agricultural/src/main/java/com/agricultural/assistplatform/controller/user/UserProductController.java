package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserProductService;
import com.agricultural.assistplatform.vo.user.ProductDetailVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-商品浏览")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserProductController {

    private final UserProductService userProductService;

    @Operation(summary = "商品搜索")
    @GetMapping("/products/search")
    public Result<PageResult<ProductSimpleVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String originPlace,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userProductService.search(keyword, sortBy, categoryId, originPlace, pageNum, pageSize));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(userProductService.getDetail(id));
    }

    @Operation(summary = "热销榜单")
    @GetMapping("/products/hot")
    public Result<List<ProductSimpleVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(userProductService.hotList(limit));
    }
}
