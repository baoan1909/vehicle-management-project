package com.ban.vehicle_management.shared.exception;

public class CaptchaValidationException extends BadRequestException {

    private final String code;

    public CaptchaValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
