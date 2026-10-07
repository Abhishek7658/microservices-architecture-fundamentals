package com.example.api_gateway;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

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

        /*
         * Step 1:
         * Check whether the client already provided
         * a correlation ID.
         */
        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        /*
         * Step 2:
         * If the client did not provide one,
         * generate a new UUID.
         */
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        /*
         * Step 3:
         * Add the correlation ID to the response.
         * This allows the client/Postman to see
         * which ID belongs to the request.
         */
        response.setHeader(
                CORRELATION_ID_HEADER,
                correlationId
        );

        /*
         * Step 4:
         * Wrap the incoming request so that
         * downstream code can retrieve the
         * correlation ID using getHeader().
         */
        HttpServletRequest wrappedRequest =
                new CorrelationIdRequestWrapper(
                        request,
                        correlationId
                );

        String method = request.getMethod();
        String url = request.getRequestURI();
        String service = identifyService(url);

        try {

            /*
             * Continue the request processing.
             */
            filterChain.doFilter(
                    wrappedRequest,
                    response
            );

        } finally {

            /*
             * Step 5:
             * Calculate total request processing time.
             */
            long responseTime =
                    System.currentTimeMillis() - startTime;

            /*
             * Step 6:
             * Write structured request information
             * to the Gateway logs.
             */
            logger.info(
                    "Correlation ID: {} | Method: {} | URL: {} | Service: {} | Status: {} | Response Time: {} ms",
                    correlationId,
                    method,
                    url,
                    service,
                    response.getStatus(),
                    responseTime
            );
        }
    }

    /*
     * Identifies which microservice is responsible
     * for the requested Gateway route.
     */
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

    /*
     * Request wrapper used to make the generated
     * or supplied correlation ID available through
     * the request headers.
     */
    private static class CorrelationIdRequestWrapper
            extends HttpServletRequestWrapper {

        private final String correlationId;

        public CorrelationIdRequestWrapper(
                HttpServletRequest request,
                String correlationId) {

            super(request);
            this.correlationId = correlationId;
        }

        /*
         * Returns our correlation ID when a service
         * asks for X-Correlation-ID.
         */
        @Override
        public String getHeader(String name) {

            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
                return correlationId;
            }

            return super.getHeader(name);
        }

        /*
         * Returns the correlation ID through getHeaders().
         */
        @Override
        public Enumeration<String> getHeaders(String name) {

            if (CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {

                return Collections.enumeration(
                        Collections.singletonList(
                                correlationId
                        )
                );
            }

            return super.getHeaders(name);
        }

        /*
         * Makes X-Correlation-ID appear in the list
         * of available request headers.
         */
        @Override
        public Enumeration<String> getHeaderNames() {

            var headers =
                    Collections.list(
                            super.getHeaderNames()
                    );

            if (headers.stream()
                    .noneMatch(
                            h -> CORRELATION_ID_HEADER
                                    .equalsIgnoreCase(h)
                    )) {

                headers.add(
                        CORRELATION_ID_HEADER
                );
            }

            return Collections.enumeration(headers);
        }
    }
}