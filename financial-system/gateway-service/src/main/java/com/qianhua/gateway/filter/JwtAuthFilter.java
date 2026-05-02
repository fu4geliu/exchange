package com.qianhua.gateway.filter;

import com.qianhua.common.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import com.qianhua.gateway.config.GatewayAuthProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final List<String> whitelist;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(GatewayAuthProperties gatewayAuthProperties) {
        this.whitelist = gatewayAuthProperties.getWhitelist();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange);
        ServerWebExchange baseExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-Trace-Id", traceId)
                        .build())
                .build();

        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(baseExchange);
        }

        String header = baseExchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return writeJsonError(baseExchange, HttpStatusCode.valueOf(401), "UNAUTHORIZED", "缺少Token", traceId);
        }

        String token = header.substring("Bearer ".length());
        if (JwtUtil.isExpired(token)) {
            return writeJsonError(baseExchange, HttpStatusCode.valueOf(401), "UNAUTHORIZED", "Token已过期", traceId);
        }

        final TokenInfo tokenInfo;
        try {
            JwtUtil.TokenPayload payload = JwtUtil.parseToken(token);
            tokenInfo = new TokenInfo(payload.accountId(), payload.role(), payload.refId());
        } catch (JwtException e) {
            return writeJsonError(baseExchange, HttpStatusCode.valueOf(401), "UNAUTHORIZED", "Token无效", traceId);
        }

        if ((path.startsWith("/api/admin/") || path.startsWith("/admin/")) && !"OPERATOR".equals(tokenInfo.role)) {
            return writeJsonError(baseExchange, HttpStatusCode.valueOf(403), "FORBIDDEN", "权限不足", traceId);
        }

        ServerWebExchange mutatedExchange = baseExchange.mutate()
                .request(baseExchange.getRequest().mutate()
                        .header("X-Account-Id", tokenInfo.accountId)
                        .header("X-Role", tokenInfo.role)
                        .header("X-Ref-Id", tokenInfo.refId)
                        .build())
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isWhitelisted(String path) {
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> writeJsonError(ServerWebExchange exchange, HttpStatusCode status, String code, String message, String traceId) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);
        String body = "{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"traceId\":\"" + traceId + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String incoming = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (incoming != null && !incoming.isBlank()) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record TokenInfo(String accountId, String role, String refId) {
    }
}
