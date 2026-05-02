package com.qianhua.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public final class JwtUtil {
    private static final String DEFAULT_SECRET = "qianhua-jwt-secret-please-change-in-production-2026";
    private static final long EXPIRE_SECONDS = 2 * 60 * 60L;

    private JwtUtil() {
    }

    private static SecretKey secretKey() {
        String secret = System.getProperty("qianhua.jwt.secret");
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String issueToken(String subject) {
        return issueToken(subject, inferRole(subject), "");
    }

    public static String issueToken(String accountId, String role, String refId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(accountId)
                .claim("role", role)
                .claim("refId", refId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXPIRE_SECONDS)))
                .signWith(secretKey())
                .compact();
    }

    public static boolean validate(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseRaw(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public static boolean isExpired(String token) {
        try {
            parseRaw(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public static TokenPayload parseToken(String token) {
        Jws<Claims> jws = parseRaw(token);
        Claims claims = jws.getPayload();
        String accountId = claims.getSubject();
        String role = Objects.toString(claims.get("role"), inferRole(accountId));
        String refId = Objects.toString(claims.get("refId"), "");
        return new TokenPayload(accountId, role, refId);
    }

    private static Jws<Claims> parseRaw(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token);
    }

    private static String inferRole(String accountId) {
        if (accountId == null) {
            return "CUSTOMER";
        }
        String lower = accountId.toLowerCase();
        return (lower.contains("operator") || lower.contains("admin")) ? "OPERATOR" : "CUSTOMER";
    }

    public record TokenPayload(String accountId, String role, String refId) {
    }
}
