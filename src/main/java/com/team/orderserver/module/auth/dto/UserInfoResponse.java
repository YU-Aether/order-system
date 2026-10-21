package com.team.orderserver.module.auth.dto;

import java.io.Serializable;

public record UserInfoResponse(
        Long id,
        String username,
        String nickname,
        String role
) implements Serializable {
}
