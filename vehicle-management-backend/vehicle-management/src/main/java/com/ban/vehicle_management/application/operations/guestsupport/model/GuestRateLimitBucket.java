package com.ban.vehicle_management.application.operations.guestsupport.model;

public enum GuestRateLimitBucket {
    ASSISTANT_IP("assistant-ip"),
    ASSISTANT_SESSION("assistant-session"),
    CAPTCHA_IP("captcha-ip"),
    CAPTCHA_SESSION("captcha-session");

    private final String keySegment;

    GuestRateLimitBucket(String keySegment) {
        this.keySegment = keySegment;
    }

    public String keySegment() {
        return keySegment;
    }
}
