package com.team.orderserver.security;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.user.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 安全上下文工具类。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<LoginUser> getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }
        return Optional.empty();
    }

    public static LoginUser requireLoginUser() {
        return getLoginUser().orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED));
    }

    public static Long getCurrentUserId() {
        return getLoginUser().map(LoginUser::id).orElse(null);
    }

    public static boolean isAdmin() {
        return getLoginUser().map(user -> user.role() == UserRole.ADMIN).orElse(false);
    }
}
