package com.agricultural.assistplatform.service.merchant;

import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.dto.merchant.MerchantRegisterDTO;
import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ShopInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ShopInfoMapper;
import com.agricultural.assistplatform.service.SmsService;
import com.agricultural.assistplatform.util.JwtUtil;
import com.agricultural.assistplatform.vo.merchant.MerchantLoginVO;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantAuthService {

    private final MerchantInfoMapper merchantInfoMapper;
    private final ShopInfoMapper shopInfoMapper;
    private final SmsService smsService;
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional(rollbackFor = Exception.class)
    public MerchantLoginVO register(MerchantRegisterDTO dto) {
        if (!smsService.verifyCode(dto.getPhone(), dto.getCode()))
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        Long cnt = merchantInfoMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getPhone, dto.getPhone()));
        if (cnt > 0) throw new BusinessException(ResultCode.BAD_REQUEST, "该手机号已注册");
        MerchantInfo m = new MerchantInfo();
        m.setPhone(dto.getPhone());
        m.setMerchantName(dto.getMerchantName());
        m.setContactPerson(dto.getContactPerson());
        m.setContactPhone(dto.getContactPhone());
        m.setPassword(StrUtil.isNotBlank(dto.getPassword()) ? encoder.encode(dto.getPassword()) : null);
        m.setAuditStatus(0);
        m.setStatus(0);
        merchantInfoMapper.insert(m);
        ShopInfo shop = new ShopInfo();
        shop.setMerchantId(m.getId());
        shop.setShopName(dto.getMerchantName());
        shopInfoMapper.insert(shop);
        return buildLoginVO(m);
    }

    public MerchantLoginVO login(Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String password = body.get("password");
        if (phone == null || phone.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "手机号不能为空");
        MerchantInfo m = merchantInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantInfo>()
                        .eq(MerchantInfo::getPhone, phone));
        if (m == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请先注册");
        if (StrUtil.isNotBlank(code)) {
            if (!smsService.verifyCode(phone, code)) throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        } else if (StrUtil.isNotBlank(password)) {
            if (StrUtil.isBlank(m.getPassword()) || !encoder.matches(password, m.getPassword()))
                throw new BusinessException(ResultCode.BAD_REQUEST, "密码错误");
        } else throw new BusinessException(ResultCode.BAD_REQUEST, "请提供验证码或密码");
        if (m.getStatus() == 2) throw new BusinessException(ResultCode.FORBIDDEN, "店铺已被禁用");
        return buildLoginVO(m);
    }

    public Map<String, Object> auditStatus() {
        Long merchantId = com.agricultural.assistplatform.common.LoginContext.getUserId();
        if (merchantId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        MerchantInfo m = merchantInfoMapper.selectById(merchantId);
        if (m == null) throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        return java.util.Map.of(
                "auditStatus", m.getAuditStatus(),
                "rejectReason", m.getRejectReason() != null ? m.getRejectReason() : "",
                "status", m.getStatus());
    }

    private MerchantLoginVO buildLoginVO(MerchantInfo m) {
        String token = jwtUtil.generateToken(String.valueOf(m.getId()), "merchant");
        MerchantLoginVO vo = new MerchantLoginVO();
        vo.setToken(token);
        vo.setMerchantId(m.getId());
        vo.setMerchantName(m.getMerchantName());
        vo.setAuditStatus(m.getAuditStatus());
        vo.setStatus(m.getStatus());
        return vo;
    }
}
