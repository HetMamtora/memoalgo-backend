package com.memoalgo.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException{

    public ResourceNotFoundException(String message){
        super(message, HttpStatus.NOT_FOUND.value());
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue){
        super(
                String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND.value()
        );
    }
}
