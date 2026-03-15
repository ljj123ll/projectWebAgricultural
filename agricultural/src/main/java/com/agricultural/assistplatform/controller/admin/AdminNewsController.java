package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.News;
import com.agricultural.assistplatform.entity.NewsCategory;
import com.agricultural.assistplatform.service.admin.AdminNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员端-新闻管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminNewsController {

    private final AdminNewsService adminNewsService;

    @Operation(summary = "新闻列表")
    @GetMapping("/news")
    public Result<PageResult<News>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(adminNewsService.list(categoryId, auditStatus, pageNum, pageSize));
    }

    @Operation(summary = "新闻详情")
    @GetMapping("/news/{id}")
    public Result<News> get(@PathVariable Long id) {
        return Result.ok(adminNewsService.get(id));
    }

    @Operation(summary = "新增新闻")
    @PostMapping("/news")
    public Result<Map<String, Object>> create(@RequestBody News news) {
        Long id = adminNewsService.create(news);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改新闻")
    @PutMapping("/news/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody News news) {
        adminNewsService.update(id, news);
        return Result.ok();
    }

    @Operation(summary = "删除新闻")
    @DeleteMapping("/news/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminNewsService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "分类列表")
    @GetMapping("/news/categories")
    public Result<List<NewsCategory>> categories() {
        return Result.ok(adminNewsService.listCategories());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/news/categories")
    public Result<Map<String, Object>> createCategory(@RequestBody NewsCategory c) {
        Long id = adminNewsService.createCategory(c);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/news/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody NewsCategory c) {
        adminNewsService.updateCategory(id, c);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/news/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminNewsService.deleteCategory(id);
        return Result.ok();
    }
}
