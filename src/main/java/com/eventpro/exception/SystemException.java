package com.eventpro.exception;

/**
 * 500 Internal Server Error : erreur serveur inattendue.
 * Le message renvoye au client ne doit jamais devoiler le code source.
 */
public class SystemException extends RuntimeException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
