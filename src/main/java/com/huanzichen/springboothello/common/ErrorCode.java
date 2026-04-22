package com.huanzichen.springboothello.common;

public class ErrorCode {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    // 作用: 不允许别人new这个工具类
    private ErrorCode() {
    }
}
