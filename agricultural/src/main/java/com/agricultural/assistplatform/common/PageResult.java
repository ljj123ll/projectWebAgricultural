package com.agricultural.assistplatform.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果（与接口文档分页一致）
 */
@Data
public class PageResult<T> {
    private Long total;
    private List<T> list;

    public static <T> PageResult<T> of(Long total, List<T> list) {
        PageResult<T> p = new PageResult<>();
        p.setTotal(total);
        p.setList(list);
        return p;
    }
}
