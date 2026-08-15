package com.memoalgo.exception;

import org.springframework.http.HttpStatus;

public class DisposableEmailException extends AppException {
    public DisposableEmailException(String message) {
        super(message, HttpStatus.BAD_REQUEST.value());
    }
}