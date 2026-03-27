package com.agricultural.assistplatform.vo.common;

import lombok.Data;

@Data
public class UnsalableSummaryVO {
    private Integer totalCount;
    private Integer manualCount;
    private Integer algorithmCount;
    private Integer mixedCount;
}
