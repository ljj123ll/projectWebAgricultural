package com.agricultural.assistplatform.vo.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RegionNodeVO {
    private String value;
    private String label;
    private List<RegionNodeVO> children = new ArrayList<>();
}

