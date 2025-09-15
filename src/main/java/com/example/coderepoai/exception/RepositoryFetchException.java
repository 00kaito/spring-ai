package com.example.coderepoai.exception;

public class RepositoryFetchException extends RuntimeException {
    
    public RepositoryFetchException(String message) {
        super(message);
    }

    public RepositoryFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}