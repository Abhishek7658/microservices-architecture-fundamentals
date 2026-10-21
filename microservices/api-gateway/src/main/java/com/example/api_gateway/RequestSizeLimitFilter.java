
package com.example.api_gateway;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBodySize;

    public RequestSizeLimitFilter(
            @Value("${gateway.request.max-body-size:1048576}") long maxBodySize) {

        if (maxBodySize <= 0) {
            throw new IllegalArgumentException(
                    "gateway.request.max-body-size must be positive");
        }

        this.maxBodySize = maxBodySize;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();

        // Reject oversized requests early when Content-Length is available.
        if (contentLength > maxBodySize) {
            writePayloadTooLarge(response);
            return;
        }

        // Buffer at most maxBodySize + 1 bytes. This also detects oversized
        // requests without Content-Length, including chunked request bodies.
        byte[] body = request.getInputStream()
                .readNBytes((int) Math.min(maxBodySize + 1, Integer.MAX_VALUE));

        if (body.length > maxBodySize) {
            writePayloadTooLarge(response);
            return;
        }

        filterChain.doFilter(
                new CachedBodyRequestWrapper(request, body),
                response);
    }

    private void writePayloadTooLarge(
            HttpServletResponse response) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getOutputStream().write(("{\"status\":413,"
            + "\"error\":\"Payload Too Large\","
            + "\"message\":\"Request body exceeds the configured size limit\"}")
            .getBytes(StandardCharsets.UTF_8));
    }

    private static class CachedBodyRequestWrapper
            extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(
                HttpServletRequest request,
                byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);

            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException(
                            "Asynchronous request-body reading is not supported");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    getInputStream(), getCharacterEncoding() == null
                            ? StandardCharsets.UTF_8
                            : java.nio.charset.Charset.forName(
                                    getCharacterEncoding())));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
