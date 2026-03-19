package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.vo.admin.AdminMerchantDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminMerchantService {

    private final MerchantInfoMapper merchantInfoMapper;
    private final ShopInfoMapper shopInfoMapper;

    public PageResult<MerchantInfo> list(Integer auditStatus, Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<MerchantInfo> q = new LambdaQueryWrapper<MerchantInfo>();
        if (auditStatus != null) q.eq(MerchantInfo::getAuditStatus, auditStatus);
        if (status != null) q.eq(MerchantInfo::getStatus, status);
        q.orderByDesc(MerchantInfo::getCreateTime);
        Page<MerchantInfo> page = merchantInfoMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void updateStatus(Long id, Integer status) {
        MerchantInfo m = new MerchantInfo();
        m.setId(id);
        m.setStatus(status);
        merchantInfoMapper.updateById(m);
    }

    public AdminMerchantDetailVO detail(Long id) {
        MerchantInfo m = merchantInfoMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        ShopInfo s = shopInfoMapper.selectOne(
                new LambdaQueryWrapper<ShopInfo>().eq(ShopInfo::getMerchantId, id));
        AdminMerchantDetailVO vo = new AdminMerchantDetailVO();
        vo.setId(m.getId());
        vo.setPhone(m.getPhone());
        vo.setMerchantName(m.getMerchantName());
        vo.setContactPerson(m.getContactPerson());
        vo.setContactPhone(m.getContactPhone());
        vo.setAuditStatus(m.getAuditStatus());
        vo.setRejectReason(m.getRejectReason());
        vo.setStatus(m.getStatus());
        vo.setIdCard(m.getIdCard());
        vo.setIdCardFront(m.getIdCardFront());
        vo.setIdCardBack(m.getIdCardBack());
        vo.setLicenseImg(m.getLicenseImg());
        if (s != null) {
            vo.setShopId(s.getId());
            vo.setShopName(s.getShopName());
            vo.setShopIntro(s.getShopIntro());
            vo.setQualificationImg(s.getQualificationImg());
            vo.setShopType(s.getShopType());
            vo.setCategories(s.getCategories());
        }
        return vo;
    }
}
