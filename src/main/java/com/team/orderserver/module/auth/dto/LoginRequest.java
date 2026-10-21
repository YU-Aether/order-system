package com.team.orderserver.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) implements Serializable {
}
