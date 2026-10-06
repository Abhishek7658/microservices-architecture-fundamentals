package com.example.api_gateway;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-ID");

        if (userId == null || userId.isBlank()) {
            userId = request.getRemoteAddr();
        }

        String key = "rate_limit:user:" + userId;

        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(
                    key,
                    WINDOW_SECONDS,
                    java.util.concurrent.TimeUnit.SECONDS
            );
        }

        if (requestCount != null && requestCount > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit exceeded. Maximum 100 requests per minute.\"," +
                    "\"status\":\"429\"}"
            );

            return;
        }

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(MAX_REQUESTS)
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        Math.max(
                                0,
                                MAX_REQUESTS -
                                        (requestCount != null ? requestCount : 0)
                        )
                )
        );

        filterChain.doFilter(request, response);
    }
}