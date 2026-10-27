package com.sgl.corporate;

/** The corporate gateway could not be reached or answered something unusable. */
public class CorporateApiUnavailableException extends RuntimeException {

    public CorporateApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
