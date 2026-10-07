package order_service.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class CorrelationIdInterceptor
        implements ClientHttpRequestInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(CorrelationIdInterceptor.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution)
            throws IOException {

        RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();

        if (attributes != null) {

            Object correlationId =
                    attributes.getAttribute(
                            CORRELATION_ID_HEADER,
                            RequestAttributes.SCOPE_REQUEST
                    );

            if (correlationId != null) {

                request.getHeaders().set(
                        CORRELATION_ID_HEADER,
                        correlationId.toString()
                );

                log.info(
                        "Propagating Correlation ID: {} | Outgoing URI: {}",
                        correlationId,
                        request.getURI()
                );
            }
        }

        return execution.execute(request, body);
    }
}