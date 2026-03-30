package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserProductService;
import com.agricultural.assistplatform.entity.ProductCategory;
import com.agricultural.assistplatform.service.common.UnsalableProductService;
import com.agricultural.assistplatform.vo.common.UnsalableProductVO;
import com.agricultural.assistplatform.vo.user.ProductDetailVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.agricultural.assistplatform.vo.user.TraceArchiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-商品浏览")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
/**
 * 用户端商品控制器。
 * 答辩时如果老师问“商品搜索、商品详情、溯源档案分别从哪个接口进入”，可以先看这里。
 */
public class UserProductController {

    private final UserProductService userProductService;
    private final UnsalableProductService unsalableProductService;

    @Operation(summary = "商品搜索")
    @GetMapping("/products/search")
    public Result<PageResult<ProductSimpleVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String originPlace,
            @RequestParam(required = false) Integer isUnsalable,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userProductService.search(keyword, sortBy, categoryId, originPlace, isUnsalable, pageNum, pageSize));
    }

    @Operation(summary = "滞销帮扶商品列表")
    @GetMapping("/unsalable/products")
    public Result<PageResult<UnsalableProductVO>> unsalableProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "all") String sourceType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(unsalableProductService.list(keyword, sortBy, sourceType, pageNum, pageSize));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(userProductService.getDetail(id));
    }

    /**
     * 独立溯源档案入口。
     * 二维码扫码后最终应落到这里，再由服务层组装完整溯源信息。
     */
    @Operation(summary = "商品溯源档案")
    @GetMapping("/traces/{traceCode}")
    public Result<TraceArchiveVO> trace(@PathVariable String traceCode) {
        return Result.ok(userProductService.getTraceArchive(traceCode));
    }

    @Operation(summary = "热销榜单")
    @GetMapping("/products/hot")
    public Result<List<ProductSimpleVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(userProductService.hotList(limit));
    }

    @Operation(summary = "商品分类")
    @GetMapping("/categories")
    public Result<List<ProductCategory>> categories() {
        return Result.ok(userProductService.categories());
    }

    @Operation(summary = "产地列表")
    @GetMapping("/origins")
    public Result<List<String>> origins() {
        return Result.ok(userProductService.origins());
    }
}
