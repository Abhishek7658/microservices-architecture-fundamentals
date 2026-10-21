// package order_service.exception;

// import java.time.LocalDateTime;
// import java.util.LinkedHashMap;
// import java.util.Map;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.client.RestClientException;

// @RestControllerAdvice
// public class GlobalExceptionHandler {
//         private static final Logger log =
//         LoggerFactory.getLogger(GlobalExceptionHandler.class);
//         @ExceptionHandler(UserServiceUnavailableException.class)
// public ResponseEntity<Map<String, Object>> handleUserServiceUnavailable(
//         UserServiceUnavailableException ex) {

//     Map<String, Object> body = new LinkedHashMap<>();

//     body.put("timestamp", LocalDateTime.now());
//     body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
//     body.put("error", "User Service Unavailable");
//     body.put("message", ex.getMessage());

//     return ResponseEntity
//             .status(HttpStatus.SERVICE_UNAVAILABLE)
//             .body(body);
// }

//     @ExceptionHandler(UserNotFoundException.class)
//     public ResponseEntity<Map<String, Object>> handleUserNotFound(
//             UserNotFoundException ex) {

//         Map<String, Object> body = new LinkedHashMap<>();

//         body.put("timestamp", LocalDateTime.now());
//         body.put("status", HttpStatus.NOT_FOUND.value());
//         body.put("error", "User Not Found");
//         body.put("message", ex.getMessage());

//         return ResponseEntity
//                 .status(HttpStatus.NOT_FOUND)
//                 .body(body);
//     }

//     @ExceptionHandler(OrderNotFoundException.class)
//     public ResponseEntity<Map<String, Object>> handleOrderNotFound(
//             OrderNotFoundException ex) {

//         Map<String, Object> body = new LinkedHashMap<>();

//         body.put("timestamp", LocalDateTime.now());
//         body.put("status", HttpStatus.NOT_FOUND.value());
//         body.put("error", "Order Not Found");
//         body.put("message", ex.getMessage());

//         return ResponseEntity
//                 .status(HttpStatus.NOT_FOUND)
//                 .body(body);
//     }

//     @ExceptionHandler(RestClientException.class)
//     public ResponseEntity<Map<String, Object>> handleRestClientException(
//             RestClientException ex) {

//         Map<String, Object> body = new LinkedHashMap<>();

//         body.put("timestamp", LocalDateTime.now());
//         body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
//         body.put("error", "User Service Unavailable");
//         body.put(
//                 "message",
//                 "Unable to communicate with User Service"
//         );

//         return ResponseEntity
//                 .status(HttpStatus.SERVICE_UNAVAILABLE)
//                 .body(body);
//     }
//     @ExceptionHandler(Exception.class)
// public ResponseEntity<Map<String, Object>> handleUnexpectedException(
//         Exception ex) {

//     log.error("Unexpected error while processing request", ex);

//     Map<String, Object> body = new LinkedHashMap<>();
//     body.put("timestamp", LocalDateTime.now());
//     body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//     body.put("error", "Internal Server Error");
//     body.put("message", "An unexpected error occurred");

//     return ResponseEntity
//             .status(HttpStatus.INTERNAL_SERVER_ERROR)
//             .body(body);
// }
// }

package order_service.exception;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("traceId", request.getHeader("X-Correlation-ID"));

        return body;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError ->
                        fieldError.getField() + ": "
                                + fieldError.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed for {}: {}",
                request.getRequestURI(), message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        message.isBlank()
                                ? "Request validation failed"
                                : message,
                        request));
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceUnavailable(
            UserServiceUnavailableException ex,
            HttpServletRequest request) {

        log.warn("User Service unavailable: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "User Service Unavailable",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "User Not Found",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "Order Not Found",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientException(
            RestClientException ex,
            HttpServletRequest request) {

        log.warn("Error communicating with User Service: {}",
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "User Service Unavailable",
                        "Unable to communicate with User Service",
                        request));
    }
    @ExceptionHandler(IdempotencyConflictException.class)
public ResponseEntity<Map<String, Object>> handleIdempotencyConflict(
        IdempotencyConflictException ex,
        HttpServletRequest request) {

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(buildErrorResponse(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    ex.getMessage(),
                    request));
}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error while processing request", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request));
    }
}
