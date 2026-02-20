package com.agricultural.assistplatform.vo.user;

import lombok.Data;

import java.util.List;

/**
 * 首页：推荐商品、助农热销榜、滞销专区、助农新闻入口
 */
@Data
public class HomeVO {
    private List<ProductSimpleVO> recommendList;
    private List<ProductSimpleVO> hotList;
    private List<ProductSimpleVO> unsalableList;
    private List<NewsSimpleVO> newsList;
}
