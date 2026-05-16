package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
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
public class RefreshToken extends AuditableDomainModel {

    private UUID refreshTokenId;
    private UUID accountId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private String createdByIp;
    private String userAgent;
}

