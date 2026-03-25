package com.agricultural.assistplatform.common;

public final class MerchantWithdrawStatus {
    private MerchantWithdrawStatus() {}

    public static final int PENDING_AUDIT = 0;
    public static final int APPROVED_WAIT_TRANSFER = 1;
    public static final int REJECTED = 2;
    public static final int TRANSFER_SUCCESS = 3;
    public static final int TRANSFER_FAILED_RETRY = 4;
    public static final int TRANSFER_FAILED_MANUAL = 5;
    public static final int CANCELED = 6;
}

