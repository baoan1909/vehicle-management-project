package com.ban.vehicle_management.application.iam.account.model.command;

public record RegisterAccountCommand(
        String username,
        String email,
        String password
) {
}
