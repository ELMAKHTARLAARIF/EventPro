package com.eventpro.exception;

/**
 * 404 Not Found : evenement, billet ou reservation inexistant.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
