package com.team.orderserver.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * JWT 配置项。
 *
 * @param secret        HS256 密钥，长度必须 >= 32 字节
 * @param expireMinutes 令牌有效期（分钟）
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expireMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret 长度必须不少于 32 字节");
        }
        if (expireMinutes <= 0) {
            expireMinutes = 120;
        }
    }
}
