package com.ban.vehicle_management.application.operations.chatconversation.model;

import com.ban.vehicle_management.shared.enumeration.operations.ChatAttachmentType;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRealtimeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID attachmentId;
    private UUID messageId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private ChatAttachmentType attachmentType;
    private Integer width;
    private Integer height;
}
