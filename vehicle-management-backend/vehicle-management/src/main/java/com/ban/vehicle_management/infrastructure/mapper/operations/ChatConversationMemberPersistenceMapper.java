package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationMemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatConversationMemberPersistenceMapper {

    ChatConversationMemberEntity toEntity(ChatConversationMember domain);

    ChatConversationMember toDomain(ChatConversationMemberEntity entity);
}
