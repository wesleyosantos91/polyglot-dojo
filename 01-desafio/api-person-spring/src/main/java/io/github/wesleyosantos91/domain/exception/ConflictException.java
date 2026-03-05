package io.github.wesleyosantos91.domain.exception;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}