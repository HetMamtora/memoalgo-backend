package com.memoalgo.exception;

public abstract class AppException extends RuntimeException{

    private final int httpStatusCode;

    protected AppException(String message, int httpStatusCode){
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    protected AppException(String message, Throwable cause, int httpStatusCode){
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode(){
        return httpStatusCode;
    }
}
