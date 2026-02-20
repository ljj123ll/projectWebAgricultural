package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.entity.News;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.NewsMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.vo.user.HomeVO;
import com.agricultural.assistplatform.vo.user.NewsSimpleVO;
import com.agricultural.assistplatform.vo.user.ProductSimpleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserHomeService {

    private final ProductInfoMapper productInfoMapper;
    private final NewsMapper newsMapper;

    private static final int PAGE_SIZE = 10;

    public HomeVO getHome() {
        HomeVO vo = new HomeVO();
        vo.setRecommendList(listProducts(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1).orderByDesc(ProductInfo::getCreateTime).last("LIMIT " + PAGE_SIZE)));
        vo.setHotList(listProducts(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1).orderByDesc(ProductInfo::getSalesVolume).last("LIMIT " + PAGE_SIZE)));
        vo.setUnsalableList(listProducts(new LambdaQueryWrapper<ProductInfo>()
                .eq(ProductInfo::getStatus, 1).eq(ProductInfo::getIsUnsalable, 1).last("LIMIT " + PAGE_SIZE)));
        List<News> news = newsMapper.selectList(
                new LambdaQueryWrapper<News>().eq(News::getAuditStatus, 1).orderByDesc(News::getCreateTime).last("LIMIT 5"));
        vo.setNewsList(news.stream().map(n -> {
            NewsSimpleVO nv = new NewsSimpleVO();
            nv.setId(n.getId());
            nv.setTitle(n.getTitle());
            nv.setCoverImg(n.getCoverImg());
            nv.setCategoryId(n.getCategoryId());
            return nv;
        }).collect(Collectors.toList()));
        return vo;
    }

    private List<ProductSimpleVO> listProducts(LambdaQueryWrapper<ProductInfo> query) {
        List<ProductInfo> list = productInfoMapper.selectList(query);
        return list.stream().map(this::toSimpleVO).collect(Collectors.toList());
    }

    private ProductSimpleVO toSimpleVO(ProductInfo p) {
        ProductSimpleVO v = new ProductSimpleVO();
        v.setId(p.getId());
        v.setProductName(p.getProductName());
        v.setPrice(p.getPrice());
        v.setSalesVolume(p.getSalesVolume() != null ? p.getSalesVolume() : 0);
        v.setStock(p.getStock() != null ? p.getStock() : 0);
        v.setProductImg(p.getProductImg());
        v.setScore(p.getScore() != null ? p.getScore() : java.math.BigDecimal.ZERO);
        return v;
    }
}
