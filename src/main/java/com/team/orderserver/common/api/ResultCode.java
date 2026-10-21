package com.team.orderserver.common.api;

/**
 * 统一业务返回码。
 * <p>约定：2xx 成功，4xx 客户端错误，5xx 服务端错误，1xxx 起为业务细分码。</p>
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源状态冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "系统内部错误"),

    USERNAME_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),

    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_SKU_EXISTS(2002, "商品 SKU 已存在"),
    PRODUCT_OFF_SALE(2003, "商品已下架"),
    STOCK_NOT_ENOUGH(2004, "商品库存不足"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_INVALID(3002, "订单状态不允许该操作"),
    ORDER_EXPIRED(3003, "订单已超时关闭"),
    ORDER_NOT_OWNED(3004, "无权操作该订单"),
    DUPLICATE_REQUEST(3005, "请勿重复提交"),
    ORDER_AMOUNT_INVALID(3006, "订单金额不合法"),

    PAYMENT_FAILED(4001, "支付失败"),
    PAYMENT_DUPLICATED(4002, "订单已支付，请勿重复操作");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
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
