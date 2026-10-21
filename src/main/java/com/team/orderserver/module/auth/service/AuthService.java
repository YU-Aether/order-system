package com.team.orderserver.module.auth.service;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.constant.RedisKeys;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.auth.dto.LoginRequest;
import com.team.orderserver.module.auth.dto.LoginResponse;
import com.team.orderserver.module.auth.dto.RegisterRequest;
import com.team.orderserver.module.auth.dto.UserInfoResponse;
import com.team.orderserver.module.user.entity.User;
import com.team.orderserver.module.user.entity.UserStatus;
import com.team.orderserver.module.user.repository.UserRepository;
import com.team.orderserver.security.JwtTokenProvider;
import com.team.orderserver.security.LoginUser;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 认证领域服务：注册、登录、登出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public UserInfoResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        userRepository.save(user);
        log.info("用户注册成功 | userId={} | username={}", user.getId(), user.getUsername());
        return toUserInfo(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ResultCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), null, user.getRole());
        String token = tokenProvider.createToken(loginUser);
        log.info("用户登录成功 | userId={} | username={}", user.getId(), user.getUsername());
        return new LoginResponse(token, "Bearer", tokenProvider.getExpireSeconds(), toUserInfo(user));
    }

    /**
     * 登出：把令牌编号写入黑名单，剩余有效期到期后自动清理。
     */
    public void logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return;
        }
        try {
            Claims claims = tokenProvider.parseClaims(authorization.substring(BEARER_PREFIX.length()).trim());
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0 && claims.getId() != null) {
                redisTemplate.opsForValue().set(RedisKeys.TOKEN_BLACKLIST + claims.getId(), "1",
                        Duration.ofMillis(remaining));
            }
        } catch (Exception ex) {
            log.debug("登出令牌解析失败: {}", ex.getMessage());
        }
    }

    private UserInfoResponse toUserInfo(User user) {
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getNickname(), user.getRole().name());
    }
}
