package com.ban.vehicle_management.domain.operations.guestsupport.model;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestSupportRequest {
    private UUID requestId;
    private String publicReferenceCode;
    private UUID categoryId;
    private String title;
    private String content;
    private String email;
    private String phone;
    private String contactHash;
    private String ipHash;
    private String sessionHash;
    private String idempotencyHash;
    private String capabilityTokenHash;
    private Instant capabilityExpiresAt;
    private Instant retentionExpiresAt;
    private SupportTicketStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
