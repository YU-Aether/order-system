package com.team.orderserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 订单业务配置。
 *
 * @param timeoutMinutes 待支付超时时间（分钟）
 * @param pageMaxSize    分页最大条数
 */
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(int timeoutMinutes, int pageMaxSize) {

    public OrderProperties {
        if (timeoutMinutes <= 0) {
            timeoutMinutes = 15;
        }
        if (pageMaxSize <= 0) {
            pageMaxSize = 100;
        }
    }
}
