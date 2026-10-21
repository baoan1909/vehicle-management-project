package com.ban.vehicle_management.shared.exception;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;

public class UserGuideException extends RuntimeException {

    private final UserGuideErrorCode errorCode;

    public UserGuideException(UserGuideErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserGuideErrorCode getErrorCode() {
        return errorCode;
    }
}
