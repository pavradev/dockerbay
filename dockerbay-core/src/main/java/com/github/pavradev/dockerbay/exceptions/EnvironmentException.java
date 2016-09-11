package com.github.pavradev.dockerbay.exceptions;

/**
 * Exception thrown by the Environment
 */
public class EnvironmentException extends RuntimeException {
    public EnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnvironmentException(String message) {
        super(message);
    }
}
