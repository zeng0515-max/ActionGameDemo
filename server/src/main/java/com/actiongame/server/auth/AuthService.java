package com.actiongame.server.auth;

public class AuthService {
    private final String requiredToken;

    public AuthService() {
        this(System.getenv("AUTH_TOKEN"));
    }

    public AuthService(String requiredToken) {
        this.requiredToken = requiredToken;
    }

    public boolean isTokenRequired() {
        return requiredToken != null && !requiredToken.isBlank();
    }

    public boolean validate(String token) {
        return !isTokenRequired() || requiredToken.equals(token);
    }
}
