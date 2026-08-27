package com.ban.vehicle_management.infrastructure.persistence.database.projection.operations;

import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import java.util.UUID;

public interface ChatConversationParticipantProjection {

    UUID getConversationId();

    UUID getAccountId();

    ChatMemberRole getMemberRole();

    String getUsername();

    String getEmail();

    String getFullName();

    String getAvatarObjectKey();

    String getAccountRoleCode();

    String getAccountRoleName();
}
