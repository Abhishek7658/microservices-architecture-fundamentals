package com.example.api_gateway;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

@Component
public class GatewayLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(GatewayLoggingFilter.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Make the correlation ID available in Gateway logs.
        MDC.put("correlationId", correlationId);

        try {
            HttpServletRequest wrappedRequest =
                    new CorrelationIdRequestWrapper(
                            request,
                            correlationId
                    );

            // FIX: the proxied (downstream) response headers are written with
            // addHeader(), which APPENDS to the value set here. This wrapper
            // turns any addHeader() for X-Correlation-ID into setHeader(), so
            // the response always carries exactly one value.
            HttpServletResponse wrappedResponse =
                    new SingleCorrelationIdResponseWrapper(
                            response,
                            correlationId
                    );

            String method = request.getMethod();
            String url = request.getRequestURI();
            String service = identifyService(url);

            logger.info(
                    "Authorization header present: {}",
                    wrappedRequest.getHeader("Authorization") != null
            );

            logger.info(
                    "Idempotency-Key received by Gateway: {}",
                    wrappedRequest.getHeader("Idempotency-Key")
            );

            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // NOTE: do NOT set the header again here. By this point the
            // response is already committed, so a setHeader() call is ignored.

            logger.info(
                    "Request completed | correlationId={} | method={} | "
                            + "url={} | service={} | status={} | responseTimeMs={}",
                    correlationId,
                    method,
                    url,
                    service,
                    response.getStatus(),
                    System.currentTimeMillis() - startTime
            );

        } catch (ServletException | IOException | RuntimeException e) {
            logger.error(
                    "Request failed | correlationId={} | method={} | url={}",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    e
            );
            throw e;

        } finally {
            MDC.remove("correlationId");
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

    private static class CorrelationIdRequestWrapper
            extends HttpServletRequestWrapper {

        private final String correlationId;

        private CorrelationIdRequestWrapper(
                HttpServletRequest request,
                String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                return correlationId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(
                        Collections.singletonList(correlationId)
                );
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            var headers = Collections.list(super.getHeaderNames());

            if (headers.stream().noneMatch(
                    h -> CORRELATION_ID_HEADER.equalsIgnoreCase(h))) {
                headers.add(CORRELATION_ID_HEADER);
            }

            return Collections.enumeration(headers);
        }
    }

    /**
     * Guarantees a single X-Correlation-ID value on the response, no matter
     * how many times the downstream proxy tries to add it.
     */
    private static class SingleCorrelationIdResponseWrapper
            extends HttpServletResponseWrapper {

        private final String correlationId;

        private SingleCorrelationIdResponseWrapper(
                HttpServletResponse response,
                String correlationId) {
            super(response);
            this.correlationId = correlationId;
            super.setHeader(CORRELATION_ID_HEADER, correlationId);
        }

        @Override
        public void addHeader(String name, String value) {
            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                // Replace instead of append; always keep the gateway's value.
                super.setHeader(CORRELATION_ID_HEADER, correlationId);
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                super.setHeader(CORRELATION_ID_HEADER, correlationId);
                return;
            }
            super.setHeader(name, value);
        }
    }
}