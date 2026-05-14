package com.ban.vehicle_management.infrastructure.persistence.iam.account;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity extends AuditableEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_profile_id", nullable = false, unique = true)
    private UUID userProfileId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "hash_password", nullable = false)
    private String hashPassword;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

}
