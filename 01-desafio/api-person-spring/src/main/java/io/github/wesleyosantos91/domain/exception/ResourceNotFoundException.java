package io.github.wesleyosantos91.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("RESOURCE_NOT_FOUND", resourceName + " not found for identifier: " + identifier);
    }
}