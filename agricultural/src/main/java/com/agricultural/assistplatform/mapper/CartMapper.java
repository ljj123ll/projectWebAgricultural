package com.agricultural.assistplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agricultural.assistplatform.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND product_id = #{productId} AND delete_flag = 1")
    int purgeDeletedByUserAndProduct(Long userId, Long productId);
}
