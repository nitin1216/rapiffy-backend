package com.example.rapiffy.common;

import org.springframework.http.HttpStatus;

/**
 * Custom exception — throw this anywhere in the app to return a clean JSON error.
 * Example: throw new ApiException("Phone already registered", HttpStatus.CONFLICT);
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
