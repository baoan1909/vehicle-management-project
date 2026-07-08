package com.ban.vehicle_management.infrastructure.realtime.chat;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationMemberRepository;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatRealtimeRabbitConsumer {

    private final ChatConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRealtimeRabbitConsumer(
            ChatConversationMemberRepository memberRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "#{chatRealtimeQueue.name}")
    public void handle(ChatRealtimeEvent event) {
        memberRepository.findActiveMemberAccountIds(event.conversationId())
                .forEach(accountId -> sendToUser(accountId, event));
    }

    private void sendToUser(UUID accountId, ChatRealtimeEvent event) {
        String user = accountId.toString();
        messagingTemplate.convertAndSendToUser(
                user,
                "/queue/chat/conversations/" + event.conversationId(),
                event
        );
        messagingTemplate.convertAndSendToUser(user, "/topic/chat", event);
    }
}
