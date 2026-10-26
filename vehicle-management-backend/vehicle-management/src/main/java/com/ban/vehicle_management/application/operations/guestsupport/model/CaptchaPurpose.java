package com.ban.vehicle_management.application.operations.guestsupport.model;

public enum CaptchaPurpose {
    INTAKE("support_intake"),
    ASSISTANT("support_assistant");

    private final String providerAction;

    CaptchaPurpose(String providerAction) {
        this.providerAction = providerAction;
    }

    public String providerAction() {
        return providerAction;
    }
}
