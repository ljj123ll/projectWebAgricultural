package com.agricultural.assistplatform.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 结算：选中购物车项或直接商品+数量，收货地址
 */
@Data
public class CreateOrderDTO {
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;
    /** 购物车ID列表（二选一）；若为空则用 productItems */
    private List<Long> cartIds;
    /** 直接下单：商品ID与数量 */
    private List<OrderProductItem> productItems;

    @Data
    public static class OrderProductItem {
        private Long productId;
        private Integer productNum;
    }
}
