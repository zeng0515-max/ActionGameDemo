package com.actiongame.server.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("鉴权服务")
class AuthServiceTest {

    @Test
    @DisplayName("should_acceptAnyToken_whenNotRequired")
    void should_acceptAnyToken_whenNotRequired() {
        AuthService authService = new AuthService(null);

        assertThat(authService.isTokenRequired()).isFalse();
        assertThat(authService.validate("anything")).isTrue();
    }

    @Test
    @DisplayName("should_acceptOnlyExpectedToken_whenRequired")
    void should_acceptOnlyExpectedToken_whenRequired() {
        AuthService authService = new AuthService("server-secret");

        assertThat(authService.isTokenRequired()).isTrue();
        assertThat(authService.validate("server-secret")).isTrue();
        assertThat(authService.validate("wrong")).isFalse();
    }
}
