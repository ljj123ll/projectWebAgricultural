package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUnsalableService {

    private final ProductInfoMapper productInfoMapper;

    public PageResult<ProductInfo> list(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ProductInfo>()
                        .eq(ProductInfo::getIsUnsalable, 1)
                        .eq(ProductInfo::getDeleteFlag, 0)
                        .orderByDesc(ProductInfo::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void setUnsalable(Long id, Integer isUnsalable) {
        ProductInfo p = productInfoMapper.selectById(id);
        if (p != null) {
            p.setIsUnsalable(isUnsalable);
            productInfoMapper.updateById(p);
        }
    }
}