package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessagePersistenceMapper {

    ChatMessageEntity toEntity(ChatMessage domain);

    @Mapping(target = "attachments", ignore = true)
    ChatMessage toDomain(ChatMessageEntity entity);
}
