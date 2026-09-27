package org.tfl.exception;

import lombok.Getter;


@Getter
public class TflHttpException extends Exception {

    private final int statusCode;

    public TflHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}