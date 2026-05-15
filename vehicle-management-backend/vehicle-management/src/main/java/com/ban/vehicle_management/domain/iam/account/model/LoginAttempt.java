package com.ban.vehicle_management.domain.iam.account.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    private UUID loginAttemptId;
    private UUID accountId;
    private String usernameOrEmail;
    private Boolean success;
    private String failureReason;
    private String ipAddress;
    private String userAgent;
    private Instant attemptedAt;
}

