package com.kiotviet.fashion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception khi không tìm thấy resource.
 * → GlobalExceptionHandler map thành HTTP 404 NOT FOUND.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Long resourceId;

    public ResourceNotFoundException(String resourceName, Long resourceId) {
        super(String.format("%s không tìm thấy với id: %d", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Unknown";
        this.resourceId = null;
    }
}
