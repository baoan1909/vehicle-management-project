package com.ban.vehicle_management.application.ai.service;

public class AiLocalBusyException extends RuntimeException {
    public AiLocalBusyException() { super("LOCAL_BUSY"); }
}
