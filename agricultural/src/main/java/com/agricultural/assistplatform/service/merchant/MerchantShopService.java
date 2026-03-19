package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.vo.merchant.ShopDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantShopService {

    private final ShopInfoMapper shopInfoMapper;
    private final MerchantInfoMapper merchantInfoMapper;

    public ShopDetailVO get() {
        Long merchantId = LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        ShopInfo shopInfo = shopInfoMapper.selectOne(new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, merchantId));
        MerchantInfo merchantInfo = merchantInfoMapper.selectById(merchantId);
        
        ShopDetailVO vo = new ShopDetailVO();
        if (shopInfo != null) {
            vo.setShopName(shopInfo.getShopName());
            vo.setShopIntro(shopInfo.getShopIntro());
            vo.setQualificationImg(shopInfo.getQualificationImg());
            vo.setShopType(shopInfo.getShopType());
            vo.setCategories(shopInfo.getCategories());
            vo.setShopAddress(shopInfo.getShopAddress());
        }
        if (merchantInfo != null) {
            vo.setContactName(merchantInfo.getContactPerson());
            vo.setContactPhone(merchantInfo.getContactPhone());
            vo.setOwnerName(merchantInfo.getContactPerson());
            vo.setIdCard(merchantInfo.getIdCard());
            vo.setIdCardFront(merchantInfo.getIdCardFront());
            vo.setIdCardBack(merchantInfo.getIdCardBack());
            vo.setLicenseImg(merchantInfo.getLicenseImg());
            vo.setAuditStatus(merchantInfo.getAuditStatus());
            vo.setRejectReason(merchantInfo.getRejectReason());
        }
        return vo;
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
            s.setShopType(body.getShopType());
            s.setCategories(body.getCategories());
            s.setShopAddress(body.getShopAddress());
            shopInfoMapper.insert(s);
        } else {
            exist.setShopName(body.getShopName());
            exist.setShopIntro(body.getShopIntro());
            exist.setQualificationImg(body.getQualificationImg());
            exist.setShopType(body.getShopType());
            exist.setCategories(body.getCategories());
            exist.setShopAddress(body.getShopAddress());
            shopInfoMapper.updateById(exist);
        }
    }
}
