package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response;

import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatConversationParticipantUserResponse {

    private UUID accountId;
    private ChatMemberRole memberRole;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String accountRoleCode;
}
