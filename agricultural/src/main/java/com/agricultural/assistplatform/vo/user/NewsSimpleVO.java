package com.agricultural.assistplatform.vo.user;

import lombok.Data;

@Data
public class NewsSimpleVO {
    private Long id;
    private String title;
    private String coverImg;
    private Long categoryId;
}
