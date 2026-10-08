package order_service.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.nanoTime();

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        /*
         * Store the correlation ID so that the
         * RestTemplate interceptor can access it
         * during outgoing HTTP calls.
         */
        request.setAttribute(
                CORRELATION_ID_HEADER,
                correlationId
        );

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long durationMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            logger.info(
                    "Correlation ID: {} | Method: {} | URI: {} | Status: {} | Duration: {} ms",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
        }
    }
}