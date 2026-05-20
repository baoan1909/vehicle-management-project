package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
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
public class AccountStatusHistory {

    private UUID accountStatusHistoryId;
    private UUID accountId;
    private AccountStatus oldStatus;
    private AccountStatus newStatus;
    private String reason;
    private Instant changedAt;
    private UUID changedBy;
}

