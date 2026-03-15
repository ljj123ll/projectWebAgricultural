package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.entity.News;
import com.agricultural.assistplatform.entity.NewsCategory;
import com.agricultural.assistplatform.service.user.UserNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-助农新闻")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserNewsController {

    private final UserNewsService userNewsService;

    @Operation(summary = "新闻列表")
    @GetMapping("/news")
    public Result<PageResult<News>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(userNewsService.list(categoryId, pageNum, pageSize));
    }

    @Operation(summary = "新闻详情")
    @GetMapping("/news/{id}")
    public Result<News> detail(@PathVariable Long id) {
        return Result.ok(userNewsService.detail(id));
    }

    @Operation(summary = "新闻分类")
    @GetMapping("/news/categories")
    public Result<List<NewsCategory>> categories() {
        return Result.ok(userNewsService.categories());
    }
}
