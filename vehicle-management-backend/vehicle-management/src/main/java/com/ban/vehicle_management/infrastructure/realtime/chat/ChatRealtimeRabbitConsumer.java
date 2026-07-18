package com.ban.vehicle_management.infrastructure.realtime.chat;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeMessage;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationMemberRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatRealtimeRabbitConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRealtimeRabbitConsumer.class);

    private final ChatConversationPortOut chatPortOut;
    private final ChatConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRealtimeEventMapper realtimeEventMapper;

    public ChatRealtimeRabbitConsumer(
            ChatConversationPortOut chatPortOut,
            ChatConversationMemberRepository memberRepository,
            SimpMessagingTemplate messagingTemplate,
            ChatRealtimeEventMapper realtimeEventMapper
    ) {
        this.chatPortOut = chatPortOut;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
        this.realtimeEventMapper = realtimeEventMapper;
    }

    @RabbitListener(queues = "#{chatRealtimeQueue.name}")
    public void handle(ChatRealtimeEvent event) {
        ChatRealtimeEvent resolvedEvent = resolveCompleteEvent(event);
        if (resolvedEvent == null || resolvedEvent.conversationId() == null) {
            LOGGER.warn("Skip invalid chat realtime event {}", event);
            return;
        }

        memberRepository.findActiveMemberAccountIds(resolvedEvent.conversationId())
                .forEach(accountId -> sendToUser(accountId, resolvedEvent));
    }

    private ChatRealtimeEvent resolveCompleteEvent(ChatRealtimeEvent event) {
        if (isComplete(event)) {
            return event;
        }

        if (event == null || event.messageId() == null) {
            return event;
        }

        return chatPortOut.findMessageById(event.messageId())
                .map(message -> realtimeEventMapper.toRealtimeEvent(message, event.occurredAt()))
                .orElseGet(() -> {
                    LOGGER.warn("Cannot hydrate chat realtime event because message {} was not found", event.messageId());
                    return event;
                });
    }

    private boolean isComplete(ChatRealtimeEvent event) {
        if (event == null || event.conversationId() == null || event.messageId() == null) {
            return false;
        }

        ChatRealtimeMessage message = event.message();
        return message != null
                && message.getMessageId() != null
                && message.getConversationId() != null
                && StringUtils.hasText(message.getCreatedAt());
    }

    private void sendToUser(UUID accountId, ChatRealtimeEvent event) {
        String user = accountId.toString();
        messagingTemplate.convertAndSendToUser(
                user,
                "/queue/chat/conversations/" + event.conversationId(),
                event
        );
        messagingTemplate.convertAndSendToUser(user, "/queue/chat", event);
        messagingTemplate.convertAndSendToUser(user, "/topic/chat", event);
    }
}
