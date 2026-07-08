package com.ban.vehicle_management.application.operations.chatconversation.port.out;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;

public interface ChatRealtimeEventPublisherPortOut {

    void publish(ChatRealtimeEvent event);
}
