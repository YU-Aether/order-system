package com.team.orderserver.module.auth.dto;

import java.io.Serializable;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfoResponse user
) implements Serializable {
}
