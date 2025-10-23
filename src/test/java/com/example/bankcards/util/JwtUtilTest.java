package com.example.bankcards.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    public void setUp() {
        String secret = Base64.getEncoder().encodeToString("secret1234567890secret1234567890".getBytes());
        jwtUtil = new JwtUtil(secret, Duration.ofHours(1));
    }

    @Test
    void generateToken_shouldContainUsernameAndRoles() {
        List<GrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails user = new User("Иван Иванов", "password", roles);

        String token = jwtUtil.generateToken(user);

        assertThat(token).isNotBlank();

        String username = jwtUtil.getUsername(token);
        List<String> parsedRoles = jwtUtil.getRoles(token);

        assertThat(username).isEqualTo("Иван Иванов");
        assertThat(parsedRoles).containsExactly("ROLE_USER");
    }

    @Test
    void generatedToken_shouldExpireAfterConfiguredDuration() {
        List<GrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails user = new User("Иван Иванов", "pass", roles);

        String token = jwtUtil.generateToken(user);

        Claims claims = ReflectionTestUtils.invokeMethod(jwtUtil, "getClaimsFromToken", token);
        long expMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(expMillis).isEqualTo(Duration.ofHours(1).toMillis());
    }

    @Test
    void getUsername_shouldReturnExpectedValue() {
        UserDetails user = new User("Иван Иванов", "pass", List.of());
        String token = jwtUtil.generateToken(user);

        String username = jwtUtil.getUsername(token);

        assertThat(username).isEqualTo("Иван Иванов");
    }

}
