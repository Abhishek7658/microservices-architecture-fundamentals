package com.example.api_gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

@Component
public class GatewayLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(GatewayLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        response.setHeader(REQUEST_ID_HEADER, requestId);

        HttpServletRequest wrappedRequest =
                new RequestIdRequestWrapper(request, requestId);

        String method = request.getMethod();
        String url = request.getRequestURI();
        String service = identifyService(url);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;

            logger.info(
                    "Request ID: {} | Method: {} | URL: {} | Service: {} | Status: {} | Response Time: {} ms",
                    requestId,
                    method,
                    url,
                    service,
                    response.getStatus(),
                    responseTime
            );
        }
    }

    private String identifyService(String url) {

        if (url.startsWith("/api/users")) {
            return "user-service";
        }

        if (url.startsWith("/api/orders")) {
            return "order-service";
        }

        if (url.startsWith("/api/products")) {
            return "product-service";
        }

        if (url.startsWith("/api/payments")) {
            return "payment-service";
        }

        return "unknown";
    }

    private static class RequestIdRequestWrapper
            extends HttpServletRequestWrapper {

        private final String requestId;

        public RequestIdRequestWrapper(
                HttpServletRequest request,
                String requestId) {

            super(request);
            this.requestId = requestId;
        }

        @Override
        public String getHeader(String name) {

            if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                return requestId;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {

            if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(
                        Collections.singletonList(requestId)
                );
            }

            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {

            var headers = Collections.list(super.getHeaderNames());

            if (headers.stream()
                    .noneMatch(h -> REQUEST_ID_HEADER.equalsIgnoreCase(h))) {
                headers.add(REQUEST_ID_HEADER);
            }

            return Collections.enumeration(headers);
        }
    }
}