package com.ban.vehicle_management.application.ai.service;

public class AiRuntimeModeChangedException extends RuntimeException {
    public AiRuntimeModeChangedException() { super("AI_PROVIDER_MODE_UNAVAILABLE"); }
}
