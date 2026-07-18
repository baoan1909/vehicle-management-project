package com.ban.vehicle_management.application.operations.chatconversation.mapper;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatAttachmentReadUrl;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationParticipant;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatInboxItem;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatConversationParticipantUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatConversationUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatInboxItemUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatAttachmentReadUrlResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatAttachmentUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatMessageUserResponse;
import java.time.Instant;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ChatConversationApiMapper {

    @Mapping(target = "lastMessageAt", source = "lastMessageAt", qualifiedByName = "formatInstant")
    ChatConversationUserResponse toConversationUserResponse(ChatConversation conversation);

    List<ChatConversationUserResponse> toConversationUserResponses(List<ChatConversation> conversations);

    ChatConversationParticipantUserResponse toParticipantUserResponse(ChatConversationParticipant participant);

    List<ChatConversationParticipantUserResponse> toParticipantUserResponses(List<ChatConversationParticipant> participants);

    ChatInboxItemUserResponse toInboxItemUserResponse(ChatInboxItem item);

    List<ChatInboxItemUserResponse> toInboxItemUserResponses(List<ChatInboxItem> items);

    @Mapping(target = "deletedAt", source = "deletedAt", qualifiedByName = "formatInstant")
    @Mapping(target = "editedAt", source = "editedAt", qualifiedByName = "formatInstant")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatInstant")
    ChatMessageUserResponse toMessageUserResponse(ChatMessage message);

    List<ChatMessageUserResponse> toMessageUserResponses(List<ChatMessage> messages);

    @AfterMapping
    default void hideDeletedContent(ChatMessage message, @MappingTarget ChatMessageUserResponse response) {
        if (message != null && message.isDeleted()) {
            response.setContent(null);
        }
    }

    ChatAttachmentUserResponse toAttachmentUserResponse(ChatMessageAttachment attachment);

    List<ChatAttachmentUserResponse> toAttachmentUserResponses(List<ChatMessageAttachment> attachments);

    ChatAttachmentReadUrlResponse toReadUrlResponse(ChatAttachmentReadUrl readUrl);

    @Named("formatInstant")
    default String formatInstant(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
