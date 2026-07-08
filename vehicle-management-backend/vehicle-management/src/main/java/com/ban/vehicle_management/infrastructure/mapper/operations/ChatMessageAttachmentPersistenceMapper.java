package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageAttachmentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMessageAttachmentPersistenceMapper {

    ChatMessageAttachmentEntity toEntity(ChatMessageAttachment domain);

    ChatMessageAttachment toDomain(ChatMessageAttachmentEntity entity);
}
