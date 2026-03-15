package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantShopService {

    private final ShopInfoMapper shopInfoMapper;

    public ShopInfo get() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return shopInfoMapper.selectOne(new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId));
    }

    public void update(ShopInfo body) {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ShopInfo exist = shopInfoMapper.selectOne(new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId));
        if (exist == null) {
            ShopInfo s = new ShopInfo();
            s.setMerchantId(merchantId);
            s.setShopName(body.getShopName());
            s.setShopIntro(body.getShopIntro());
            s.setQualificationImg(body.getQualificationImg());
            shopInfoMapper.insert(s);
        } else {
            exist.setShopName(body.getShopName());
            exist.setShopIntro(body.getShopIntro());
            exist.setQualificationImg(body.getQualificationImg());
            shopInfoMapper.updateById(exist);
        }
    }
}
