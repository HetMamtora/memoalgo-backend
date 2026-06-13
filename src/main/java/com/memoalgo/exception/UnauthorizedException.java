package com.memoalgo.exception;

import org.springframework.http.HttpStatus;

/**
 * UnauthorizedException — thrown when authentication fails.
 *
 * Examples:
 *   - Wrong password during login
 *   - Invalid JWT token
 *   - Missing JWT token on protected endpoint
 *
 * HTTP Status: 401 Unauthorized
 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED.value());
    }
}