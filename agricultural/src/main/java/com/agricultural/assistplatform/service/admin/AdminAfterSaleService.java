package com.agricultural.assistplatform.service.admin;

import com.agricultural.assistplatform.common.PageResult;
import com.agricultural.assistplatform.entity.AfterSale;
import com.agricultural.assistplatform.mapper.AfterSaleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAfterSaleService {

    private final AfterSaleMapper afterSaleMapper;

    public PageResult<AfterSale> list(Integer afterSaleStatus, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<AfterSale>();
        if (afterSaleStatus != null) q.eq(AfterSale::getAfterSaleStatus, afterSaleStatus);
        q.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void handle(Long id, Map<String, Object> body) {
        AfterSale as = afterSaleMapper.selectById(id);
        if (as == null) return;
        if (body.get("afterSaleStatus") != null) {
            as.setAfterSaleStatus(Integer.valueOf(body.get("afterSaleStatus").toString()));
        }
        if (body.get("handleResult") != null) {
            as.setHandleResult(String.valueOf(body.get("handleResult")));
        }
        if (body.get("adminId") != null) {
            as.setAdminId(Long.valueOf(body.get("adminId").toString()));
        }
        afterSaleMapper.updateById(as);
    }
}
