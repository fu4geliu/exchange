package com.example.customerservice.auth;

import com.example.customerservice.security.JwtTokenProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final long expirationMs;

    public AuthController(
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        // 最小可运行实现：固定账号。后续可改为数据库校验。
        if (!"operator".equals(request.username()) || !"123456".equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken("1", request.username(), List.of("OPERATOR"));
        return new LoginResponse(token, "Bearer", expirationMs / 1000, request.username());
    }
}
