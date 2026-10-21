package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount {

    private UUID ledgerAccountId;
    private String accountCode;
    private LedgerAccountType accountType;
    private UUID walletId;
    private UUID organizationId;
    private String currency;
    private String status;
}
