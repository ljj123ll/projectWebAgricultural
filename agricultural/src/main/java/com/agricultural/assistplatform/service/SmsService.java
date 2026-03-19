package com.agricultural.assistplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信服务（模拟：实际对接短信网关）
 * Redis key: sms:code:{phone}, 5分钟过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final String KEY_PREFIX = "sms:code:";
    private static final int EXPIRE_MINUTES = 5;
    /** 开发环境固定验证码，便于测试 */
    private static final String DEV_CODE = "123456";

    private final StringRedisTemplate redisTemplate;

    /**
     * 发送验证码
     */
    public void sendCode(String phone) {
        String code = DEV_CODE;
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("发送验证码到 {}: {}", phone, code);
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String phone, String code) {
        String key = KEY_PREFIX + phone;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) return false;
        boolean ok = cached.equals(code);
        if (ok) redisTemplate.delete(key);
        return ok;
    }

    /**
     * 发送库存预警短信
     */
    public void sendStockWarning(String phone, String productName, Integer currentStock) {
        // 模拟发送短信
        log.info("【短信提醒】发送库存预警短信到 {}: 商品 [{}] 库存仅剩 {} 件，请及时补货", 
                phone, productName, currentStock);
        
        // 实际对接短信网关时取消注释
        // String content = String.format("【助农电商】您的商品 [%s] 库存仅剩 %d 件，请及时补货。", productName, currentStock);
        // sendSms(phone, content);
    }

    /**
     * 发送新订单提醒
     */
    public void sendNewOrderAlert(String phone, String orderNo) {
        log.info("【短信提醒】发送新订单提醒到 {}: 您有新订单 [{}]，请及时处理", phone, orderNo);
        
        // String content = String.format("【助农电商】您有新订单 [%s]，请及时处理。", orderNo);
        // sendSms(phone, content);
    }

    /**
     * 发送售后提醒
     */
    public void sendAfterSaleAlert(String phone, String afterSaleNo) {
        log.info("【短信提醒】发送售后提醒到 {}: 您有新的售后申请 [{}]，请及时处理", phone, afterSaleNo);
        
        // String content = String.format("【助农电商】您有新的售后申请 [%s]，请及时处理。", afterSaleNo);
        // sendSms(phone, content);
    }

    /**
     * 发送审核结果通知
     */
    public void sendAuditResult(String phone, String businessType, boolean approved, String reason) {
        String result = approved ? "已通过" : "未通过";
        log.info("【短信提醒】发送审核结果到 {}: 您的 {} 审核{}", phone, businessType, result);
        
        // String content = String.format("【助农电商】您的 %s 审核%s。%s", businessType, result, 
        //         approved ? "" : "原因：" + reason);
        // sendSms(phone, content);
    }

    /**
     * 实际发送短信（对接短信网关时使用）
     */
    private void sendSms(String phone, String content) {
        // TODO: 对接阿里云/腾讯云短信服务
        // 示例：
        // SmsSendRequest request = new SmsSendRequest();
        // request.setPhone(phone);
        // request.setContent(content);
        // smsClient.send(request);
        
        log.info("发送短信到 {}: {}", phone, content);
    }
}
