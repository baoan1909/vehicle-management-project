package com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatAttachmentReadUrlResponse {
    private UUID attachmentId;
    private String readUrl;
    private int expireSeconds;
}
