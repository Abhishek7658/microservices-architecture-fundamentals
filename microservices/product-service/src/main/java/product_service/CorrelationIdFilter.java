package product_service;

import java.io.IOException;

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

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        logger.info(
                "Correlation ID: {} | Method: {} | URI: {}",
                correlationId,
                request.getMethod(),
                request.getRequestURI()
        );

        filterChain.doFilter(request, response);
    }
}