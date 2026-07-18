package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationParticipant;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.operations.ChatConversationParticipantProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ChatConversationParticipantPersistenceMapper {

    @Autowired
    private StorageUrlResolver storageUrlResolver;

    @Mapping(target = "avatarUrl", source = "avatarObjectKey", qualifiedByName = "resolveAvatarUrl")
    public abstract ChatConversationParticipant toDomain(ChatConversationParticipantProjection projection);

    @Named("resolveAvatarUrl")
    protected String resolveAvatarUrl(String avatarObjectKey) {
        return storageUrlResolver.resolvePublicAvatarUrl(avatarObjectKey);
    }
}
