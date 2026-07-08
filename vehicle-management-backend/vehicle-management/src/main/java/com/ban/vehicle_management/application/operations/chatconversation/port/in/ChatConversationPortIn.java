package com.ban.vehicle_management.application.operations.chatconversation.port.in;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatAttachmentReadUrl;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatInboxItem;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ChatConversationPortIn {

    List<ChatInboxItem> getInbox();

    ChatConversation getConversation(UUID conversationId);

    ChatConversation createOrGetInternalDirectConversation(UUID targetAccountId);

    ChatConversation createInternalGroupConversation(String title, Set<UUID> memberAccountIds);

    ChatConversation createOrGetCustomerSupportConversation(UUID customerId, String title);

    ChatConversation addMember(UUID conversationId, UUID accountId);

    void removeMember(UUID conversationId, UUID accountId);

    List<ChatMessage> getMessageHistory(UUID conversationId, Instant beforeCreatedAt, int limit);

    ChatMessage sendTextMessage(UUID conversationId, String content, UUID replyToMessageId);

    ChatMessage sendImageMessage(UUID conversationId, String content, List<MultipartFile> files);

    void deleteMessage(UUID messageId);

    void markRead(UUID conversationId, UUID messageId);

    ChatAttachmentReadUrl createAttachmentReadUrl(UUID attachmentId);
}
