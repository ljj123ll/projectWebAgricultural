package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.News;
import com.agricultural.assistplatform.entity.NewsCategory;
import com.agricultural.assistplatform.mapper.NewsCategoryMapper;
import com.agricultural.assistplatform.mapper.NewsMapper;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNewsService {

    private final NewsMapper newsMapper;
    private final NewsCategoryMapper newsCategoryMapper;
    private final PublicDataCacheService publicDataCacheService;

    public PageResult<News> list(Long categoryId, Integer auditStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<News>();
        if (categoryId != null) q.eq(News::getCategoryId, categoryId);
        if (auditStatus != null) q.eq(News::getAuditStatus, auditStatus);
        q.orderByDesc(News::getCreateTime);
        Page<News> page = newsMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public News get(Long id) {
        return newsMapper.selectById(id);
    }

    public Long create(News news) {
        if (news.getAuditStatus() == null) news.setAuditStatus(1);
        newsMapper.insert(news);
        publicDataCacheService.evictNews();
        return news.getId();
    }

    public void update(Long id, News news) {
        news.setId(id);
        newsMapper.updateById(news);
        publicDataCacheService.evictNews();
    }

    public void delete(Long id) {
        newsMapper.deleteById(id);
        publicDataCacheService.evictNews();
    }

    public List<NewsCategory> listCategories() {
        return newsCategoryMapper.selectList(new LambdaQueryWrapper<NewsCategory>().orderByAsc(NewsCategory::getId));
    }

    public Long createCategory(NewsCategory c) {
        newsCategoryMapper.insert(c);
        publicDataCacheService.evictNews();
        return c.getId();
    }

    public void updateCategory(Long id, NewsCategory c) {
        c.setId(id);
        newsCategoryMapper.updateById(c);
        publicDataCacheService.evictNews();
    }

    public void deleteCategory(Long id) {
        newsCategoryMapper.deleteById(id);
        publicDataCacheService.evictNews();
    }
}
