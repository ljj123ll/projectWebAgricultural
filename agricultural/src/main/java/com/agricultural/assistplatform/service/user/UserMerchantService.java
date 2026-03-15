package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMerchantService {

    private final ShopInfoMapper shopInfoMapper;
    private final ProductInfoMapper productInfoMapper;

    public ShopInfo getShop(Long merchantId) {
        return shopInfoMapper.selectOne(new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId));
    }

    public PageResult<ProductInfo> products(Long merchantId, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        Page<ProductInfo> page = productInfoMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ProductInfo>().eq(ProductInfo::getMerchantId, merchantId).eq(ProductInfo::getStatus, 1));
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}
