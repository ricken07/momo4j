package com.rickenbazolo.momo4j.core.exception;

/**
 * Exception description
 *
 * @author Ricken Bazolo
 * @since 0.1.0
 */
/**
 * Exception thrown when an HTTP request fails.
 */
public class MoMoHttpException extends MoMo4JException {

    public MoMoHttpException(String message) {
        super(message);
    }

    public MoMoHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public MoMoHttpException(int statusCode, String message) {
        super(message);
    }

    public MoMoHttpException(int statusCode, String message, Throwable cause) {
        super(message, cause);
    }
}
