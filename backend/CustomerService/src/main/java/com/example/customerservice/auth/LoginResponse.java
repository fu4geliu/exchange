package com.example.customerservice.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username) {
}
