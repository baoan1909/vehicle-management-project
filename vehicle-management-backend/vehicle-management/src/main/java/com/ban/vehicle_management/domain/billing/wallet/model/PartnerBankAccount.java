package com.ban.vehicle_management.domain.billing.wallet.model;

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
public class PartnerBankAccount {

    private UUID bankAccountId;
    private UUID organizationId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private boolean verified;
    private Instant verifiedAt;
    private UUID verifiedBy;
    private String status;
}
