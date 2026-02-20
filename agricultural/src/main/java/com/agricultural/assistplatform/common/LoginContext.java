package com.agricultural.assistplatform.common;

/**
 * 当前登录用户/商家/管理员 ID，由 Filter 设置
 */
public final class LoginContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> LOGIN_TYPE = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setLoginType(String type) {
        LOGIN_TYPE.set(type);
    }

    public static String getLoginType() {
        return LOGIN_TYPE.get();
    }

    public static void clear() {
        USER_ID.remove();
        LOGIN_TYPE.remove();
    }
}
