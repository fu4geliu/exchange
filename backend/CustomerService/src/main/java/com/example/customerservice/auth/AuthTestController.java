package com.example.customerservice.auth;

import com.example.customerservice.security.JwtAuthenticationFilter.AuthUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
public class AuthTestController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        return Map.of(
                "userId", principal.userId(),
                "username", principal.username(),
                "authorities", authentication.getAuthorities());
    }
}
