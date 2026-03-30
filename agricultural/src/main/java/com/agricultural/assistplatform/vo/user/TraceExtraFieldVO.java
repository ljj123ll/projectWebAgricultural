package com.agricultural.assistplatform.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceExtraFieldVO {
    private String key;
    private String label;
    private String value;
}
