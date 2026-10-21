package com.team.orderserver.security;

import com.team.orderserver.module.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            new JwtProperties("0123456789012345678901234567890123456789", 60));

    @Test
    void shouldCreateAndParseToken() {
        String token = provider.createToken(new LoginUser(7L, "alice", null, UserRole.ADMIN));

        Claims claims = provider.parseClaims(token);

        assertEquals("7", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertNotNull(claims.getId());
        assertEquals(3600, provider.getExpireSeconds());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = provider.createToken(new LoginUser(1L, "bob", null, UserRole.USER));

        assertThrows(JwtException.class, () -> provider.parseClaims(token + "tampered"));
    }

    @Test
    void shouldRejectShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties("too-short", 60));
    }
}
