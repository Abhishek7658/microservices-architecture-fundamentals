package order_service.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${user-service.connect-timeout-ms:2000}")
    private int connectTimeoutMs;

    @Value("${user-service.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        return builder
                .connectTimeout(
                        Duration.ofMillis(connectTimeoutMs)
                )
                .readTimeout(
                        Duration.ofMillis(readTimeoutMs)
                )
                .additionalInterceptors(
                        new CorrelationIdInterceptor()
                )
                .build();
    }
}