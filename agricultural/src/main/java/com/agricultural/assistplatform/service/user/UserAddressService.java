package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.UserAddress;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.UserAddressMapper;
import com.agricultural.assistplatform.vo.user.UserAddressVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddressVO> list() {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        List<UserAddress> list = userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Map<String, Object> body) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        UserAddress addr = new UserAddress();
        addr.setUserId(userId);
        addr.setReceiver((String) body.get("receiver"));
        addr.setPhone((String) body.get("phone"));
        addr.setProvince((String) body.get("province"));
        addr.setCity((String) body.get("city"));
        addr.setCounty((String) body.get("county"));
        addr.setTown((String) body.get("town"));
        addr.setDetailAddress((String) body.get("detailAddress"));
        addr.setIsDefault(body.get("isDefault") != null && Boolean.TRUE.equals(body.get("isDefault")) ? 1 : 0);
        if (addr.getIsDefault() == 1) {
            userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>().eq(UserAddress::getUserId, userId).set(UserAddress::getIsDefault, 0));
        }
        userAddressMapper.insert(addr);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        UserAddress addr = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getId, id).eq(UserAddress::getUserId, userId));
        if (addr == null) throw new BusinessException(ResultCode.NOT_FOUND, "地址不存在");
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>().eq(UserAddress::getUserId, userId).set(UserAddress::getIsDefault, 0));
        addr.setIsDefault(1);
        userAddressMapper.updateById(addr);
    }

    private UserAddressVO toVO(UserAddress a) {
        UserAddressVO vo = new UserAddressVO();
        vo.setId(a.getId());
        vo.setReceiver(a.getReceiver());
        vo.setPhone(a.getPhone());
        vo.setProvince(a.getProvince());
        vo.setCity(a.getCity());
        vo.setCounty(a.getCounty());
        vo.setTown(a.getTown());
        vo.setDetailAddress(a.getDetailAddress());
        vo.setIsDefault(a.getIsDefault());
        return vo;
    }
}
