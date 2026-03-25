package com.agricultural.assistplatform.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentUpdateDTO {
    @NotNull(message = "评分不能为空")
    @Min(1)
    @Max(5)
    private Integer score;
    private String content;
    private String imgUrls;
    private String mediaUrls;
}
