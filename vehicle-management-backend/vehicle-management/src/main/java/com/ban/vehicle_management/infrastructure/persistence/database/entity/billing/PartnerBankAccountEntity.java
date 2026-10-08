package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partner_bank_accounts", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartnerBankAccountEntity {

    @Id
    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "status", nullable = false)
    private String status;
}
