package com.rickenbazolo.momo4j.core.exception;

/**
 * MoMo4JException class description.
 *
 * @author Ricken Bazolo
 * @since 0.1.0
 */
public class MoMo4JException extends RuntimeException {

    public MoMo4JException(String message) {
        super(message);
    }

    public MoMo4JException(String message, Throwable cause) {
        super(message, cause);
    }

    public MoMo4JException(Throwable cause) {
        super(cause);
    }
}
