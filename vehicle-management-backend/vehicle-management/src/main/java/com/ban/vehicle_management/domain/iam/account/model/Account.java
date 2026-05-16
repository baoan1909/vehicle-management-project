package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.AccountStatus;
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
public class Account extends AuditableDomainModel {

    private UUID accountId;
    private UUID userProfileId;
    private String username;
    private String email;
    private String hashPassword;
    private UUID roleId;
    private AccountStatus status;
    private Instant lastLoginAt;
    private Integer failedLoginCount;
    private Instant lockedUntil;
    private Instant passwordChangedAt;
}

