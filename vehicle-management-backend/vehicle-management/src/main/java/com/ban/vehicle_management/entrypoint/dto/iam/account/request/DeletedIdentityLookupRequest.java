package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeletedIdentityLookupRequest(
        @NotNull DeletedIdentityType identityType,
        @NotBlank @Size(max = 500) String value,
        @NotBlank @Size(max = 500) String reason
) {}
