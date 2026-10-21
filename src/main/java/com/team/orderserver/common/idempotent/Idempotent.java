package com.team.orderserver.common.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等控制。客户端需在请求头携带 {@code Idempotency-Key}。
 * <p>同一用户 + 同一业务前缀 + 同一 Key 在窗口期内只允许成功执行一次。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 业务前缀，用于区分不同接口。 */
    String keyPrefix();

    /** 幂等窗口期，单位秒。 */
    long timeoutSeconds() default 300;

    /** 重复提交时的提示语。 */
    String message() default "请勿重复提交";
}
