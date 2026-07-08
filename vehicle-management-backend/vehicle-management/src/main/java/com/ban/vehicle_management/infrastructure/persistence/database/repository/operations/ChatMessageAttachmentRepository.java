package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageAttachmentEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachmentEntity, UUID> {

    List<ChatMessageAttachmentEntity> findByMessageIdIn(Collection<UUID> messageIds);
}
