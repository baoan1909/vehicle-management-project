package com.ban.vehicle_management.shared.enumeration.notification;

import org.springframework.http.HttpStatus;

public enum UserGuideErrorCode {
    USER_GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_GUIDE_FEATURE_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_GUIDE_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_GUIDE_STEP_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_GUIDE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    USER_GUIDE_VERSION_IMMUTABLE(HttpStatus.CONFLICT),
    USER_GUIDE_INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
    USER_GUIDE_NOT_ELIGIBLE(HttpStatus.FORBIDDEN),
    USER_GUIDE_PUBLISH_CONFLICT(HttpStatus.CONFLICT),
    USER_GUIDE_OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT),
    USER_GUIDE_INVALID_STEP(HttpStatus.BAD_REQUEST),
    USER_GUIDE_INVALID_AUDIENCE(HttpStatus.BAD_REQUEST);

    private final HttpStatus httpStatus;

    UserGuideErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
