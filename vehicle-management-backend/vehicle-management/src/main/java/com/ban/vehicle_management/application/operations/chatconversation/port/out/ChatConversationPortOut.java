package com.ban.vehicle_management.application.operations.chatconversation.port.out;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ChatConversationPortOut {

    ChatConversation saveConversation(ChatConversation conversation);

    Optional<ChatConversation> findConversationById(UUID conversationId);

    List<ChatConversation> findInboxConversations(UUID accountId);

    Optional<ChatConversation> findInternalDirectConversation(UUID firstAccountId, UUID secondAccountId);

    Optional<ChatConversation> findActiveCustomerSupportConversation(UUID customerId);

    ChatConversationMember saveMember(ChatConversationMember member);

    Optional<ChatConversationMember> findMember(UUID conversationId, UUID accountId);

    List<ChatConversationMember> findActiveMembers(UUID conversationId);

    List<UUID> findActiveMemberAccountIds(UUID conversationId);

    void removeMember(UUID conversationId, UUID accountId, Instant leftAt);

    boolean existsActiveMember(UUID conversationId, UUID accountId);

    ChatMessage saveMessage(ChatMessage message);

    Optional<ChatMessage> findMessageById(UUID messageId);

    List<ChatMessage> findMessageHistory(UUID conversationId, Instant beforeCreatedAt, int limit);

    long countUnreadMessages(UUID conversationId, UUID accountId);

    ChatMessageAttachment saveAttachment(ChatMessageAttachment attachment);

    List<ChatMessageAttachment> findAttachmentsByMessageIds(Collection<UUID> messageIds);

    Optional<ChatMessageAttachment> findAttachmentById(UUID attachmentId);

    void markRead(UUID conversationId, UUID accountId, UUID messageId);

    boolean existsActiveAccount(UUID accountId);

    boolean existsActiveInternalAccount(UUID accountId);

    Optional<UUID> findCustomerIdByAccountId(UUID accountId);

    boolean existsCustomer(UUID customerId);
}
