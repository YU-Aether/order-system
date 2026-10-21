package com.team.orderserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 初始化数据配置（首次启动时创建管理员与示例商品）。
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(String adminUsername, String adminPassword) {

    public BootstrapProperties {
        if (adminUsername == null || adminUsername.isBlank()) {
            adminUsername = "admin";
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "admin123";
        }
    }
}
