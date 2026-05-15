package com.ban.vehicle_management.infrastructure.persistence.iam.account;

import com.ban.vehicle_management.shared.enumeration.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_status_history", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusHistoryEntity {

    @Id
    @Column(name = "account_status_history_id", nullable = false)
    private UUID accountStatusHistoryId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private AccountStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private AccountStatus newStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity changedByAccount;

}
