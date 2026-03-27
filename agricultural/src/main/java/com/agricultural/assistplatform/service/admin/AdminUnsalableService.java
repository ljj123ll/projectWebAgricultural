package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.service.common.AdminRealtimeEventService;
import com.agricultural.assistplatform.service.common.PublicDataCacheService;
import com.agricultural.assistplatform.service.common.UnsalableProductService;
import com.agricultural.assistplatform.service.common.UserRealtimeEventService;
import com.agricultural.assistplatform.vo.common.UnsalableProductVO;
import com.agricultural.assistplatform.vo.common.UnsalableSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUnsalableService {

    private final ProductInfoMapper productInfoMapper;
    private final UnsalableProductService unsalableProductService;
    private final UserRealtimeEventService userRealtimeEventService;
    private final AdminRealtimeEventService adminRealtimeEventService;
    private final PublicDataCacheService publicDataCacheService;

    public PageResult<UnsalableProductVO> list(String keyword, String sortBy, String sourceType, Integer pageNum, Integer pageSize) {
        return unsalableProductService.list(keyword, sortBy, sourceType, pageNum, pageSize);
    }

    public UnsalableSummaryVO summary(String keyword) {
        return unsalableProductService.summary(keyword);
    }

    public void setUnsalable(Long id, Integer isUnsalable) {
        ProductInfo p = productInfoMapper.selectById(id);
        if (p != null) {
            p.setIsUnsalable(isUnsalable);
            productInfoMapper.updateById(p);
            publicDataCacheService.evictUnsalable();
            publicDataCacheService.evictHome();
            publicDataCacheService.evictProductSearch();
            userRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
            adminRealtimeEventService.publishRefreshToAll("UNSALABLE_UPDATED", String.valueOf(id));
        }
    }
}
