package org.tfl.exception;

import lombok.Getter;

/**
 * A non-success HTTP response from TfL. Carries the status code so callers can
 * map it to a gRPC status instead of parsing a message string.
 */
@Getter
public class TflHttpException extends Exception {

    private final int statusCode;

    public TflHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}