package com.team.orderserver.module.auth.controller;

import com.team.orderserver.common.api.Result;
import com.team.orderserver.module.auth.dto.LoginRequest;
import com.team.orderserver.module.auth.dto.LoginResponse;
import com.team.orderserver.module.auth.dto.RegisterRequest;
import com.team.orderserver.module.auth.dto.UserInfoResponse;
import com.team.orderserver.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01-认证", description = "注册、登录、登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册新用户")
    @PostMapping("/register")
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "登录并获取 JWT")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "登出（令牌加入黑名单）")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }
}
