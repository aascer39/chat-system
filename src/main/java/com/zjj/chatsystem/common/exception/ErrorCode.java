package com.zjj.chatsystem.common.exception;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // ========== 通用 ==========
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ========== 业务 ==========
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PASSWORD_WEAK(1004, "密码强度不足，需包含字母和数字，至少 8 位"),
    TOKEN_INVALID(1005, "Token 无效或已过期"),
    TOKEN_EXPIRED(1006, "Token 已过期，请重新登录"),

    // ========== 业务：聊天 ==========
    MESSAGE_TOO_LONG(2001, "消息长度超过限制"),
    RECEIVER_NOT_FOUND(2002, "接收方不存在"),

    // ========== 系统 ==========
    DB_ERROR(5001, "数据库操作失败"),
    CACHE_ERROR(5002, "缓存服务异常"),
    LOCK_FAILED(5003, "获取锁失败，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
