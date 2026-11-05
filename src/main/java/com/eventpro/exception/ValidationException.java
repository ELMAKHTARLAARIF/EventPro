package com.eventpro.exception;

/**
 * 400 Bad Request : informations incorrectes (ex. quantite negative, dates illogiques).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
