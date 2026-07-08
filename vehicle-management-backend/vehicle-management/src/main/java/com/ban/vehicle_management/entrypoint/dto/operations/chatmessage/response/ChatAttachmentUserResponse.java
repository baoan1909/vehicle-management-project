package com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response;

import com.ban.vehicle_management.shared.enumeration.operations.ChatAttachmentType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatAttachmentUserResponse {
    private UUID attachmentId;
    private UUID messageId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private ChatAttachmentType attachmentType;
    private Integer width;
    private Integer height;
}
