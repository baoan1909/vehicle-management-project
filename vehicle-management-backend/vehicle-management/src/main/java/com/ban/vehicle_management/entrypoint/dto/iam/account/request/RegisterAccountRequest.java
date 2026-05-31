package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

public record RegisterAccountRequest(
        String username,
        String email,
        String password
) {
}
