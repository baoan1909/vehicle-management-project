package com.ban.vehicle_management.domain.operations.chatmessage.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ChatAttachmentType;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageAttachment extends AuditableDomainModel {

    private UUID attachmentId;
    private UUID messageId;
    private StorageBucket bucket;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String checksumSha256;
    private ChatAttachmentType attachmentType;
    private Integer width;
    private Integer height;
}
