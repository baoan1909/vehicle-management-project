package com.ban.vehicle_management.shared.exception;

import org.springframework.security.access.AccessDeniedException;

public class ScopedAccessDeniedException extends AccessDeniedException {
    private final String code;

    public ScopedAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
