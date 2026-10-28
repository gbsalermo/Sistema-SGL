package com.sgl.corporate;

/** The corporate gateway rejected the user's credentials (HTTP 401). */
public class InvalidCorporateCredentialsException extends RuntimeException {

    public InvalidCorporateCredentialsException(String message) {
        super(message);
    }
}
