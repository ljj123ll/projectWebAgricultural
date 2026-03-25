package com.agricultural.assistplatform.common;

/**
 * 售后流程状态常量与流转判断。
 */
public final class AfterSaleFlow {

    private AfterSaleFlow() {
    }

    // 售后状态：1-待商家处理 2-待商家签收退款 3-已完成 4-管理员介入 5-已驳回 6-待用户退货
    public static final int STATUS_PENDING_MERCHANT = 1;
    public static final int STATUS_WAIT_MERCHANT_REFUND = 2;
    public static final int STATUS_RESOLVED = 3;
    public static final int STATUS_ADMIN = 4;
    public static final int STATUS_REJECTED = 5;
    public static final int STATUS_WAIT_USER_RETURN = 6;

    // 订单状态：7-售后中 8-已完成售后
    public static final int ORDER_AFTER_SALE_PROCESSING = 7;
    public static final int ORDER_AFTER_SALE_DONE = 8;

    public static boolean isActive(Integer status) {
        return status != null
                && (status == STATUS_PENDING_MERCHANT
                || status == STATUS_WAIT_MERCHANT_REFUND
                || status == STATUS_ADMIN
                || status == STATUS_WAIT_USER_RETURN);
    }

    public static boolean canUserEscalate(Integer status) {
        return status != null
                && (status == STATUS_PENDING_MERCHANT
                || status == STATUS_WAIT_MERCHANT_REFUND
                || status == STATUS_WAIT_USER_RETURN
                || status == STATUS_REJECTED);
    }

    public static boolean canMerchantHandle(Integer status) {
        return status != null && status == STATUS_PENDING_MERCHANT;
    }

    public static boolean canMerchantConfirmReturn(Integer status, Integer afterSaleType) {
        return afterSaleType != null
                && afterSaleType == 2
                && status != null
                && status == STATUS_WAIT_MERCHANT_REFUND;
    }

    public static boolean canAdminJudge(Integer status) {
        return status != null && (status == STATUS_ADMIN || status == STATUS_REJECTED);
    }

    public static boolean isValidAdminTarget(Integer status, Integer afterSaleType) {
        if (status == null) return false;
        if (status == STATUS_RESOLVED || status == STATUS_REJECTED) return true;
        return status == STATUS_WAIT_USER_RETURN && afterSaleType != null && afterSaleType == 2;
    }
}
