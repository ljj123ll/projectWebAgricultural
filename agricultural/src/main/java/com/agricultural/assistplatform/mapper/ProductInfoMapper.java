package com.agricultural.assistplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agricultural.assistplatform.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductInfoMapper extends BaseMapper<ProductInfo> {

    @Update("""
        UPDATE product_info
        SET stock = stock - #{quantity},
            sales_volume = sales_volume + #{quantity},
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{productId}
          AND status = 1
          AND delete_flag = 0
          AND stock >= #{quantity}
        """)
    int deductStockSafely(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("""
        UPDATE product_info
        SET stock = stock + #{quantity},
            sales_volume = GREATEST(sales_volume - #{quantity}, 0),
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{productId}
          AND delete_flag = 0
        """)
    int restoreStockSafely(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
