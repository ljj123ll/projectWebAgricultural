package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.News;
import com.agricultural.assistplatform.entity.NewsCategory;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.NewsCategoryMapper;
import com.agricultural.assistplatform.mapper.NewsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNewsService {

    private final NewsMapper newsMapper;
    private final NewsCategoryMapper newsCategoryMapper;

    public PageResult<News> list(Long categoryId, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<News>().eq(News::getAuditStatus, 1);
        if (categoryId != null) q.eq(News::getCategoryId, categoryId);
        q.orderByDesc(News::getCreateTime);
        Page<News> page = newsMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public News detail(Long id) {
        News n = newsMapper.selectById(id);
        if (n == null || n.getAuditStatus() == null || n.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "新闻不存在或未发布");
        }
        Integer viewCount = n.getViewCount() != null ? n.getViewCount() : 0;
        newsMapper.update(null, new LambdaUpdateWrapper<News>().eq(News::getId, id)
                .set(News::getViewCount, viewCount + 1));
        return n;
    }

    public List<NewsCategory> categories() {
        return newsCategoryMapper.selectList(new LambdaQueryWrapper<NewsCategory>().orderByAsc(NewsCategory::getId));
    }
}
