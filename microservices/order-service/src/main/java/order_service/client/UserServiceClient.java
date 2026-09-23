package order_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import order_service.dto.UserResponse;
import order_service.exception.UserNotFoundException;
import order_service.exception.UserServiceUnavailableException;

@Component
public class UserServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(UserServiceClient.class);

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

    /**
     * Calls User Service to retrieve a user.
     *
     * The Circuit Breaker protects this external service call.
     *
     * Retry is still handled inside this method for transient failures.
     */
    @CircuitBreaker(
            name = "userService",
            fallbackMethod = "getUserFallback"
    )
    public UserResponse getUserById(Long userId) {

        String url =
                userServiceUrl + "/api/users/" + userId;

        long backoff = initialBackoffMs;

        log.info(
                "User Service request started: GET {}",
                url
        );

        for (int attempt = 1;
             attempt <= maxRetryAttempts;
             attempt++) {

            try {

                UserResponse response =
                        restTemplate.getForObject(
                                url,
                                UserResponse.class
                        );

                if (attempt > 1) {
                    log.info(
                            "User Service request succeeded on attempt {}",
                            attempt
                    );
                } else {
                    log.info(
                            "User Service request succeeded"
                    );
                }

                return response;

            } catch (HttpClientErrorException.NotFound ex) {

                /*
                 * A missing user is a business-level 404.
                 *
                 * It must not be treated as User Service failure.
                 */
                log.warn(
                        "User not found: userId={}",
                        userId
                );

                throw new UserNotFoundException(userId);

            } catch (HttpClientErrorException ex) {

                /*
                 * Other 4xx errors are not retryable.
                 */
                log.warn(
                        "Non-retryable client error from User Service: {}",
                        ex.getStatusCode()
                );

                throw ex;

            } catch (
                    ResourceAccessException |
                    HttpServerErrorException ex) {

                /*
                 * These failures are considered transient and
                 * therefore eligible for retry.
                 */

                if (attempt == maxRetryAttempts) {

                    log.error(
                            "Final User Service failure after {} attempts: {}",
                            attempt,
                            ex.getClass().getSimpleName()
                    );

                    throw new UserServiceUnavailableException(
                            "User Service did not respond after "
                                    + maxRetryAttempts
                                    + " attempts"
                    );
                }

                log.warn(
                        "User Service attempt {} failed: {}. "
                                + "Retrying in {} ms",
                        attempt,
                        ex.getClass().getSimpleName(),
                        backoff
                );

                sleep(backoff);

                backoff *= 2;
            }
        }

        /*
         * Defensive fallback.
         * Normally execution never reaches here.
         */
        throw new UserServiceUnavailableException(
                "User Service call failed unexpectedly"
        );
    }

    /**
     * Circuit Breaker fallback.
     *
     * This method is invoked when the Circuit Breaker opens
     * and prevents calls from reaching User Service.
     */
    public UserResponse getUserFallback(
            Long userId,
            Throwable throwable) {

        log.error(
                "Circuit Breaker fallback triggered for userId={}. "
                        + "Cause: {}",
                userId,
                throwable.getClass().getSimpleName()
        );

        /*
         * Do not convert a genuine 404 into a service outage.
         */
        if (throwable instanceof UserNotFoundException) {

            throw (UserNotFoundException) throwable;
        }

        throw new UserServiceUnavailableException(
                "User Service is currently unavailable"
        );
    }

    /**
     * Exponential backoff between retry attempts.
     */
    private void sleep(long millis) {

        try {

            Thread.sleep(millis);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new UserServiceUnavailableException(
                    "Retry was interrupted"
            );
        }
    }
}