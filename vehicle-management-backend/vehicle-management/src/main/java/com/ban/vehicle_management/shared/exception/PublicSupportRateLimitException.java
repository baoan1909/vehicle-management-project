package com.ban.vehicle_management.shared.exception;

public class PublicSupportRateLimitException extends TooManyRequestsException {

    private final String code;
    private final long retryAfterSeconds;

    public PublicSupportRateLimitException(String code, String message, long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }

    public String code() {
        return code;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
