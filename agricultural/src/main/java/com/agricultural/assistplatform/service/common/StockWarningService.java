package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.entity.MerchantInfo;
import com.agricultural.assistplatform.entity.ProductInfo;
import com.agricultural.assistplatform.mapper.MerchantInfoMapper;
import com.agricultural.assistplatform.mapper.ProductInfoMapper;
import com.agricultural.assistplatform.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存预警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockWarningService {

    private final ProductInfoMapper productInfoMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final SmsService smsService;

    /**
     * 检查库存并发送预警
     * @param productId 商品ID
     * @param currentStock 当前库存
     */
    public void checkStockWarning(Long productId, Integer currentStock) {
        ProductInfo product = productInfoMapper.selectById(productId);
        if (product == null) {
            return;
        }

        Integer warningStock = product.getStockWarning();
        if (warningStock == null || warningStock <= 0) {
            warningStock = 10; // 默认预警值
        }

        // 库存低于预警值
        if (currentStock <= warningStock) {
            log.warn("商品 [{}] 库存预警，当前库存: {}，预警值: {}", 
                    product.getProductName(), currentStock, warningStock);
            
            // 获取商家信息
            MerchantInfo merchant = merchantInfoMapper.selectById(product.getMerchantId());
            if (merchant != null && merchant.getPhone() != null) {
                // 发送短信提醒
                smsService.sendStockWarning(merchant.getPhone(), product.getProductName(), currentStock);
            }
        }
    }

    /**
     * 订单创建时扣减库存并检查预警
     */
    public void deductStock(Long productId, Integer quantity) {
        ProductInfo product = productInfoMapper.selectById(productId);
        if (product == null) {
            return;
        }

        int newStock = product.getStock() - quantity;
        if (newStock < 0) {
            newStock = 0;
        }

        product.setStock(newStock);
        productInfoMapper.updateById(product);

        // 检查是否需要预警
        checkStockWarning(productId, newStock);
    }

    /**
     * 订单取消时恢复库存
     */
    public void restoreStock(Long productId, Integer quantity) {
        ProductInfo product = productInfoMapper.selectById(productId);
        if (product == null) {
            return;
        }

        product.setStock(product.getStock() + quantity);
        productInfoMapper.updateById(product);

        log.info("商品 [{}] 库存恢复 {}，当前库存: {}", 
                product.getProductName(), quantity, product.getStock());
    }
}
