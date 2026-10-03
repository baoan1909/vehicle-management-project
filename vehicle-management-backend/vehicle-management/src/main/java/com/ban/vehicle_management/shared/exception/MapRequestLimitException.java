package com.ban.vehicle_management.shared.exception;

import java.time.OffsetDateTime;

public class MapRequestLimitException extends TooManyRequestsException {

    private final String code;
    private final OffsetDateTime retryAfter;

    public MapRequestLimitException(String code, String message, OffsetDateTime retryAfter) {
        super(message);
        this.code = code;
        this.retryAfter = retryAfter;
    }

    public String code() {
        return code;
    }

    public OffsetDateTime retryAfter() {
        return retryAfter;
    }
}
