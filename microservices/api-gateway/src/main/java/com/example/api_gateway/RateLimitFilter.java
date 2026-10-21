
package com.example.api_gateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(RateLimitFilter.class);

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

        String key = "rate_limit:ip:" + request.getRemoteAddr();

        try {
            Long requestCount = redisTemplate.opsForValue().increment(key);

            if (requestCount == null) {
                writeServiceUnavailable(response);
                return;
            }

            if (requestCount == 1L) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            response.setHeader(
                    "X-RateLimit-Limit",
                    String.valueOf(MAX_REQUESTS));
            response.setHeader(
                    "X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, MAX_REQUESTS - requestCount)));

            if (requestCount > MAX_REQUESTS) {
                writeTooManyRequests(response);
                return;
            }

        } catch (RedisConnectionFailureException ex) {
            logger.warn("Redis unavailable; rejecting request with 503");
            writeServiceUnavailable(response);
            return;
        } catch (RuntimeException ex) {
            logger.error("Rate limiter failed; rejecting request with 503", ex);
            writeServiceUnavailable(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeServiceUnavailable(
            HttpServletResponse response) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getOutputStream().write((
                "{\"status\":503,\"error\":\"Service Unavailable\","
                + "\"message\":\"Rate limiting is temporarily unavailable\"}")
                .getBytes(StandardCharsets.UTF_8));
    }

    private void writeTooManyRequests(
            HttpServletResponse response) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getOutputStream().write((
                "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Rate limit exceeded. Maximum 100 requests per minute.\"}")
                .getBytes(StandardCharsets.UTF_8));
    }
}
