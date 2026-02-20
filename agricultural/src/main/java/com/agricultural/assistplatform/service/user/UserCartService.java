package com.agricultural.assistplatform.service.user;

import com.agricultural.assistplatform.common.LoginContext;
import com.agricultural.assistplatform.common.ResultCode;
import com.agricultural.assistplatform.entity.Cart;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.exception.BusinessException;
import com.agricultural.assistplatform.mapper.CartMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.vo.user.CartItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCartService {

    private final CartMapper cartMapper;
    private final ProductInfoMapper productInfoMapper;

    public List<CartItemVO> list() {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        List<Cart> list = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return list.stream().map(c -> {
            CartItemVO vo = new CartItemVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setProductNum(c.getProductNum());
            ProductInfo p = productInfoMapper.selectById(c.getProductId());
            if (p != null) {
                vo.setProductName(p.getProductName());
                vo.setProductImg(p.getProductImg());
                vo.setPrice(p.getPrice());
                vo.setStock(p.getStock());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Long productId, Integer num) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        if (num == null || num < 1) num = 1;
        ProductInfo p = productInfoMapper.selectById(productId);
        if (p == null || p.getStatus() != 1) throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在或已下架");
        int stock = p.getStock() != null ? p.getStock() : 0;
        if (stock < num) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).eq(Cart::getProductId, productId));
        if (exist != null) {
            int newNum = exist.getProductNum() + num;
            if (stock < newNum) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
            exist.setProductNum(newNum);
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setProductNum(num);
            cart.setSelectStatus(1);
            cartMapper.insert(cart);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateNum(Long cartId, Integer num) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        Cart c = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getId, cartId).eq(Cart::getUserId, userId));
        if (c == null) throw new BusinessException(ResultCode.NOT_FOUND, "购物车项不存在");
        if (num == null || num < 1) num = 1;
        ProductInfo p = productInfoMapper.selectById(c.getProductId());
        int stock = p != null && p.getStock() != null ? p.getStock() : 0;
        if (stock < num) throw new BusinessException(ResultCode.BAD_REQUEST, "当前仅 " + stock + " 件可购");
        c.setProductNum(num);
        cartMapper.updateById(c);
    }

    public void delete(Long cartId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getId, cartId).eq(Cart::getUserId, userId));
    }
}
