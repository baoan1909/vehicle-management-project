package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ChatAttachmentType;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_message_attachments", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageAttachmentEntity extends AuditableEntity {

    @Id
    @Column(name = "attachment_id", nullable = false)
    private UUID attachmentId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bucket", nullable = false)
    private StorageBucket bucket;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false)
    private ChatAttachmentType attachmentType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;
}
