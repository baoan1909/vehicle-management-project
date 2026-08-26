package com.ban.vehicle_management.domain.operations.chatconversation.model;

import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationParticipant {

    private UUID conversationId;
    private UUID accountId;
    private ChatMemberRole memberRole;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String accountRoleCode;
}
