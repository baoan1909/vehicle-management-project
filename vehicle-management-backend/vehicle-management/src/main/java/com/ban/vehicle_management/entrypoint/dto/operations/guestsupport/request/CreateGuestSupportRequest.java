package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateGuestSupportRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String content,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^$|^\\+?[0-9][0-9 .-]{7,18}$") String phone,
        @Size(max = 100) String sessionId
) {
}
