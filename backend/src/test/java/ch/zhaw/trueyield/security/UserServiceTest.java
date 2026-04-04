package ch.zhaw.trueyield.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private final UserService userService = new UserService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setJwtAuthentication(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of())
        );
    }

    // ── getCurrentUserId ──────────────────────────────────────────────────────

    @Test
    void getCurrentUserId_returnsSubject_whenAuthenticated() {
        setJwtAuthentication(Map.of("sub", "user-123", "email", "test@example.com"));
        assertEquals("user-123", userService.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_returnsAnonymous_whenNotAuthenticated() {
        assertEquals("anonymous", userService.getCurrentUserId());
    }

    // ── getCurrentUserEmail ───────────────────────────────────────────────────

    @Test
    void getCurrentUserEmail_returnsEmail_whenAuthenticated() {
        setJwtAuthentication(Map.of("sub", "user-123", "email", "test@example.com"));
        assertEquals("test@example.com", userService.getCurrentUserEmail());
    }

    @Test
    void getCurrentUserEmail_returnsNull_whenNotAuthenticated() {
        assertNull(userService.getCurrentUserEmail());
    }

    // ── userHasRole ───────────────────────────────────────────────────────────

    @Test
    void userHasRole_returnsTrue_whenRolePresent() {
        setJwtAuthentication(Map.of(
                "sub", "user-123",
                "user_roles", List.of("fund-manager", "esg-auditor")
        ));
        assertTrue(userService.userHasRole("fund-manager"));
    }

    @Test
    void userHasRole_returnsFalse_whenRoleMissing() {
        setJwtAuthentication(Map.of(
                "sub", "user-123",
                "user_roles", List.of("esg-auditor")
        ));
        assertFalse(userService.userHasRole("fund-manager"));
    }

    @Test
    void userHasRole_returnsFalse_whenNoRolesClaim() {
        setJwtAuthentication(Map.of("sub", "user-123"));
        assertFalse(userService.userHasRole("fund-manager"));
    }

    @Test
    void userHasRole_returnsFalse_whenNotAuthenticated() {
        assertFalse(userService.userHasRole("fund-manager"));
    }
}
