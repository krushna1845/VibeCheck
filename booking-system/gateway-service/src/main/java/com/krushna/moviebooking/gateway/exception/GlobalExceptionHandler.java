package com.krushna.moviebooking.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    public ProblemDetail handleGatewayException(GatewayException ex) {
        log.error("Gateway exception: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "Gateway Error", ex.getMessage(), "gateway-error");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", "internal-error");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String errorCode) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://moviebooking.com/errors/" + errorCode));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("errorCode", errorCode);
        return pd;
    }
}
