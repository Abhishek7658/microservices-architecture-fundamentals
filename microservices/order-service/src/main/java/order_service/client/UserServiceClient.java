/*package order_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import order_service.dto.UserResponse;
import order_service.exception.UserNotFoundException;

@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;

    private final String userServiceUrl =
            "http://localhost:8090";

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserResponse getUserById(Long userId) {

        String url =
                userServiceUrl + "/api/users/" + userId;

        try {

            return restTemplate.getForObject(
                    url,
                    UserResponse.class
            );

        } catch (HttpClientErrorException.NotFound ex) {

            throw new UserNotFoundException(userId);

        } catch (RestClientException ex) {

            throw ex;
        }
    }
}
*/
package order_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import order_service.dto.UserResponse;
import order_service.exception.UserNotFoundException;
import order_service.exception.UserServiceUnavailableException;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${resilience.user-service.base-url}")
    private String userServiceUrl;

    @Value("${resilience.user-service.max-retry-attempts}")
    private int maxRetryAttempts;

    @Value("${resilience.user-service.initial-backoff-ms}")
    private long initialBackoffMs;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserResponse getUserById(Long userId) {

        String url = userServiceUrl + "/api/users/" + userId;
        long backoff = initialBackoffMs;

        log.info("Request started: GET {}", url);

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                UserResponse response = restTemplate.getForObject(url, UserResponse.class);

                if (attempt > 1) {
                    log.info("User Service call succeeded on attempt {}", attempt);
                }
                return response;

            } catch (HttpClientErrorException.NotFound ex) {
                // Non-retryable: the user genuinely does not exist. Retrying will not help.
                throw new UserNotFoundException(userId);

            } catch (HttpClientErrorException ex) {
                // Non-retryable: any other 4xx means our request was malformed or unauthorized,
                // not a transient failure. Retrying would just repeat the same bad request.
                log.warn("Non-retryable client error from User Service: {}", ex.getStatusCode());
                throw ex;

            } catch (ResourceAccessException | HttpServerErrorException ex) {
                // Retryable: connection issues, timeouts (ResourceAccessException),
                // and 5xx server errors (HttpServerErrorException) are typically transient.
                if (attempt == maxRetryAttempts) {
                    log.error(
                            "Final failure after {} attempts calling User Service: {}",
                            attempt, ex.getClass().getSimpleName()
                    );
                    throw new UserServiceUnavailableException(
                            "User Service did not respond after " + maxRetryAttempts + " attempts"
                    );
                }

                log.warn(
                        "Retry attempt {} failed ({}). Retrying in {} ms",
                        attempt, ex.getClass().getSimpleName(), backoff
                );
                sleep(backoff);
                backoff *= 2; // exponential backoff
            }
        }

        // Unreachable: the loop always returns or throws above.
        throw new UserServiceUnavailableException("User Service call failed unexpectedly");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserServiceUnavailableException("Retry was interrupted");
        }
    }
}