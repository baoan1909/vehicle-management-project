package com.ban.vehicle_management.application.operations.chatconversation.mapper;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeAttachment;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import java.time.Instant;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ChatRealtimeEventMapper {

    @Mapping(target = "conversationId", source = "message.conversationId")
    @Mapping(target = "messageId", source = "message.messageId")
    @Mapping(target = "message", source = "message")
    ChatRealtimeEvent toRealtimeEvent(ChatMessage message, Instant occurredAt);

    ChatRealtimeMessage toRealtimeMessage(ChatMessage message);

    @AfterMapping
    default void hideDeletedContent(ChatMessage message, @MappingTarget ChatRealtimeMessage response) {
        if (message != null && message.isDeleted()) {
            response.setContent(null);
        }
    }

    ChatRealtimeAttachment toRealtimeAttachment(ChatMessageAttachment attachment);

}
