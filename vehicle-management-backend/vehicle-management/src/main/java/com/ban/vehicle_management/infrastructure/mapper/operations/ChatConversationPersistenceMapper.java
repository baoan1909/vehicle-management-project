package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatConversationPersistenceMapper {

    ChatConversationEntity toEntity(ChatConversation domain);

    ChatConversation toDomain(ChatConversationEntity entity);
}
