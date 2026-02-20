package com.agricultural.assistplatform.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentSubmitDTO {
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    @NotNull(message = "评分不能为空")
    @Min(1) @Max(5)
    private Integer score;
    private String content;
    private String imgUrls;
}
