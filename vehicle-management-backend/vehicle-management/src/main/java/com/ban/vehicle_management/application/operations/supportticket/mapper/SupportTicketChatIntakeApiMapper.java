package com.ban.vehicle_management.application.operations.supportticket.mapper;

import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatConversationApiMapper;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketChatIntakeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SupportTicketApiMapper.class, ChatConversationApiMapper.class})
public interface SupportTicketChatIntakeApiMapper {

    @Mapping(target = "ticket", source = "ticket")
    @Mapping(target = "conversation", source = "conversation")
    SupportTicketChatIntakeResponse toResponse(SupportTicketChatIntake intake);
}
