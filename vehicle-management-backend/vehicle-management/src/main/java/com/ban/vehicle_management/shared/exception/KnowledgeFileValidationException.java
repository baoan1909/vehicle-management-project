package com.ban.vehicle_management.shared.exception;

/**
 * Upload-time knowledge file rejection that carries a stable failure code so the API
 * can return a machine-readable reason in addition to the localized message.
 */
public class KnowledgeFileValidationException extends BadRequestException {

    private final String code;

    public KnowledgeFileValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}