package com.zjj.chatsystem.common.constant;

/**
 * Redis Key 构建工具。
 * 所有 Key 必须通过此工具生成，禁止在代码中直接拼接字符串。
 */
public final class RedisKeys {

    private static final String PREFIX = "cs";

    public static String userToken(String token) {
        return join("user", "token", token);
    }

    public static String userDetail(Long userId) {
        return join("user", "detail", String.valueOf(userId));
    }

    public static String msgList(Long userId, Integer page) {
        return join("msg", "list", String.valueOf(userId), String.valueOf(page));
    }

    public static String onlineUsers() {
        return join("user", "online");
    }

    public static String lockKey(String domain, String bizId) {
        return join("lock", domain, bizId);
    }

    public static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }

    private RedisKeys() {
    }
}
