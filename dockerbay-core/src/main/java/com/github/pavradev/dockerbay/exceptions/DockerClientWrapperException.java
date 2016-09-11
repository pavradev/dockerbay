package com.github.pavradev.dockerbay.exceptions;

/**
 * Exception thrown by DockerClientWrapper
 */
public class DockerClientWrapperException extends RuntimeException {

    public DockerClientWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerClientWrapperException(String message) {
        super(message);
    }
}
