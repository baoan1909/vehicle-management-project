package com.ban.vehicle_management.application.operations.chatconversation.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatAttachmentReadUrl;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.port.in.ChatConversationPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatInboxItem;
import com.ban.vehicle_management.domain.operations.chatconversation.policy.ChatConversationPolicy;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatAttachmentPolicy;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.shared.enumeration.operations.ChatAttachmentType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatConversationUseCaseImpl implements ChatConversationPortIn {

    private static final int DEFAULT_HISTORY_LIMIT = 30;
    private static final int MAX_HISTORY_LIMIT = 100;
    private static final int ATTACHMENT_READ_URL_EXPIRE_SECONDS = 900;

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ChatConversationPortOut chatPortOut;
    private final ChatRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final FileStoragePort fileStoragePort;
    private final FileAccessPort fileAccessPort;
    private final ChatConversationPolicy conversationPolicy = new ChatConversationPolicy();
    private final ChatMessagePolicy messagePolicy = new ChatMessagePolicy();
    private final ChatAttachmentPolicy attachmentPolicy = new ChatAttachmentPolicy();

    public ChatConversationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ChatConversationPortOut chatPortOut,
            ChatRealtimeEventPublisherPortOut realtimeEventPublisher,
            FileStoragePort fileStoragePort,
            FileAccessPort fileAccessPort
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.chatPortOut = chatPortOut;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.fileStoragePort = fileStoragePort;
        this.fileAccessPort = fileAccessPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatInboxItem> getInbox() {
        UUID currentAccountId = requireCurrentAccountId();
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_READ_OWN");
        return chatPortOut.findInboxConversations(currentAccountId).stream()
                .map(conversation -> new ChatInboxItem(
                        conversation,
                        resolveLastMessage(conversation),
                        chatPortOut.countUnreadMessages(conversation.getConversationId(), currentAccountId)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatConversation getConversation(UUID conversationId) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        requireReadAccess(conversation);
        return conversation;
    }

    @Override
    @Transactional
    public ChatConversation createOrGetInternalDirectConversation(UUID targetAccountId) {
        UUID currentAccountId = requireCurrentAccountId();
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_CREATE_OWN");
        if (!chatPortOut.existsActiveInternalAccount(currentAccountId)
                || !chatPortOut.existsActiveInternalAccount(targetAccountId)) {
            throw new BadRequestException("Internal direct conversation requires active internal accounts");
        }

        return chatPortOut.findInternalDirectConversation(currentAccountId, targetAccountId)
                .orElseGet(() -> createInternalDirectConversation(currentAccountId, targetAccountId));
    }

    @Override
    @Transactional
    public ChatConversation createInternalGroupConversation(String title, Set<UUID> memberAccountIds) {
        UUID currentAccountId = requireCurrentAccountId();
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_CREATE_OWN");

        Set<UUID> participantIds = new LinkedHashSet<>();
        if (memberAccountIds != null) {
            participantIds.addAll(memberAccountIds);
        }
        participantIds.add(currentAccountId);

        ChatConversation conversation = new ChatConversation();
        conversation.setTitle(title);
        conversationPolicy.initializeInternalGroup(conversation, currentAccountId, participantIds);

        for (UUID participantId : participantIds) {
            if (!chatPortOut.existsActiveInternalAccount(participantId)) {
                throw new BadRequestException("Internal group conversation requires active internal accounts");
            }
        }

        conversation.setConversationId(UUID.randomUUID());
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        participantIds.forEach(accountId -> saveMember(
                savedConversation.getConversationId(),
                accountId,
                accountId.equals(currentAccountId) ? ChatMemberRole.OWNER : ChatMemberRole.MEMBER
        ));
        return savedConversation;
    }

    @Override
    @Transactional
    public ChatConversation createOrGetCustomerSupportConversation(UUID customerId, String title) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_CREATE_OWN");
        UUID resolvedCustomerId = resolveCustomerIdForSupportConversation(currentAccount, customerId);

        return chatPortOut.findActiveCustomerSupportConversation(resolvedCustomerId)
                .orElseGet(() -> createCustomerSupportConversation(currentAccount.accountId(), resolvedCustomerId, title));
    }

    @Override
    @Transactional
    public ChatConversation addMember(UUID conversationId, UUID accountId) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        requireManageMembersAccess(conversation);
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            throw new BadRequestException("Closed conversation cannot be updated");
        }
        if (!chatPortOut.existsActiveAccount(accountId)) {
            throw new BadRequestException("accountId must reference an active account");
        }
        if (chatPortOut.existsActiveMember(conversationId, accountId)) {
            return conversation;
        }
        saveMember(conversationId, accountId, ChatMemberRole.MEMBER);
        return conversation;
    }

    @Override
    @Transactional
    public void removeMember(UUID conversationId, UUID accountId) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        requireManageMembersAccess(conversation);
        if (Objects.equals(conversation.getOwnerAccountId(), accountId)) {
            throw new BadRequestException("Conversation owner cannot be removed");
        }
        chatPortOut.removeMember(conversationId, accountId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessageHistory(UUID conversationId, Instant beforeCreatedAt, int limit) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        requireReadAccess(conversation);
        return chatPortOut.findMessageHistory(conversationId, beforeCreatedAt, normalizeLimit(limit));
    }

    @Override
    @Transactional
    public ChatMessage sendTextMessage(UUID conversationId, String content, UUID replyToMessageId) {
        UUID currentAccountId = requireCurrentAccountId();
        requireSendAccess(conversationId);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderAccountId(currentAccountId);
        message.setContent(content);
        message.setReplyToMessageId(resolveReplyMessageId(conversationId, replyToMessageId));
        messagePolicy.initializeText(message);

        return saveMessageAndPublish(message);
    }

    @Override
    @Transactional
    public ChatMessage sendImageMessage(UUID conversationId, String content, List<MultipartFile> files) {
        UUID currentAccountId = requireCurrentAccountId();
        currentAccountPortIn.requirePermission("CHAT_ATTACHMENT_CREATE_OWN");
        requireSendAccess(conversationId);
        attachmentPolicy.validateImageFiles(files);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderAccountId(currentAccountId);
        message.setContent(content);
        messagePolicy.initializeImage(message);
        ChatMessage savedMessage = saveMessageWithoutRealtime(message);

        List<ChatMessageAttachment> attachments = files.stream()
                .map(file -> storeImageAttachment(savedMessage.getMessageId(), currentAccountId, file))
                .map(chatPortOut::saveAttachment)
                .toList();
        savedMessage.setAttachments(attachments);
        publishAfterCommit(savedMessage);
        return savedMessage;
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId) {
        UUID currentAccountId = requireCurrentAccountId();
        ChatMessage message = chatPortOut.findMessageById(messageId)
                .orElseThrow(() -> new NotFoundException("Chat message not found"));
        requireActiveMember(message.getConversationId(), currentAccountId);
        boolean canModerate = currentAccountPortIn.hasPermission("CHAT_MESSAGE_MODERATE_ALL");
        if (!Objects.equals(message.getSenderAccountId(), currentAccountId) && !canModerate) {
            throw new AccessDeniedException("Access is denied");
        }
        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        chatPortOut.saveMessage(message);
    }

    @Override
    @Transactional
    public void markRead(UUID conversationId, UUID messageId) {
        UUID currentAccountId = requireCurrentAccountId();
        ChatConversation conversation = getConversationOrThrow(conversationId);
        requireActiveMember(conversationId, currentAccountId);
        UUID resolvedMessageId = messageId == null ? conversation.getLastMessageId() : messageId;
        if (resolvedMessageId == null) {
            return;
        }
        ChatMessage message = chatPortOut.findMessageById(resolvedMessageId)
                .orElseThrow(() -> new NotFoundException("Chat message not found"));
        if (!conversationId.equals(message.getConversationId())) {
            throw new BadRequestException("messageId does not belong to conversation");
        }
        chatPortOut.markRead(conversationId, currentAccountId, resolvedMessageId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatAttachmentReadUrl createAttachmentReadUrl(UUID attachmentId) {
        currentAccountPortIn.requirePermission("CHAT_ATTACHMENT_READ_OWN");
        ChatMessageAttachment attachment = chatPortOut.findAttachmentById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Chat attachment not found"));
        ChatMessage message = chatPortOut.findMessageById(attachment.getMessageId())
                .orElseThrow(() -> new NotFoundException("Chat message not found"));
        requireReadAccess(getConversationOrThrow(message.getConversationId()));
        return new ChatAttachmentReadUrl(
                attachmentId,
                fileAccessPort.createReadUrl(attachment.getObjectKey(), ATTACHMENT_READ_URL_EXPIRE_SECONDS),
                ATTACHMENT_READ_URL_EXPIRE_SECONDS
        );
    }

    private ChatConversation createInternalDirectConversation(UUID currentAccountId, UUID targetAccountId) {
        ChatConversation conversation = new ChatConversation();
        conversationPolicy.initializeInternalDirect(conversation, currentAccountId, targetAccountId);
        conversation.setConversationId(UUID.randomUUID());
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveMember(savedConversation.getConversationId(), currentAccountId, ChatMemberRole.OWNER);
        saveMember(savedConversation.getConversationId(), targetAccountId, ChatMemberRole.MEMBER);
        return savedConversation;
    }

    private ChatConversation createCustomerSupportConversation(UUID currentAccountId, UUID customerId, String title) {
        ChatConversation conversation = new ChatConversation();
        conversation.setTitle(title);
        conversationPolicy.initializeCustomerSupport(conversation, currentAccountId, customerId);
        conversation.setConversationId(UUID.randomUUID());
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveMember(savedConversation.getConversationId(), currentAccountId, ChatMemberRole.CUSTOMER);
        return savedConversation;
    }

    private ChatConversationMember saveMember(UUID conversationId, UUID accountId, ChatMemberRole role) {
        ChatConversationMember member = chatPortOut.findMember(conversationId, accountId)
                .orElseGet(ChatConversationMember::new);
        if (member.getConversationMemberId() == null) {
            member.setConversationMemberId(UUID.randomUUID());
            member.setConversationId(conversationId);
            member.setAccountId(accountId);
            member.setJoinedAt(Instant.now());
        }
        member.setMemberRole(role);
        member.setStatus(ChatMemberStatus.ACTIVE);
        member.setLeftAt(null);
        return chatPortOut.saveMember(member);
    }

    private ChatMessage saveMessageAndPublish(ChatMessage message) {
        ChatMessage savedMessage = saveMessageWithoutRealtime(message);
        publishAfterCommit(savedMessage);
        return savedMessage;
    }

    private ChatMessage saveMessageWithoutRealtime(ChatMessage message) {
        message.setMessageId(UUID.randomUUID());
        ChatMessage savedMessage = chatPortOut.saveMessage(message);
        ChatConversation conversation = getConversationOrThrow(savedMessage.getConversationId());
        conversation.setLastMessageId(savedMessage.getMessageId());
        conversation.setLastMessageAt(savedMessage.getCreatedAt() == null ? Instant.now() : savedMessage.getCreatedAt());
        chatPortOut.saveConversation(conversation);
        return savedMessage;
    }

    private void publishAfterCommit(ChatMessage message) {
        TransactionalEvents.runAfterCommit(() -> realtimeEventPublisher.publish(new ChatRealtimeEvent(
                message.getConversationId(),
                message.getMessageId(),
                Instant.now()
        )));
    }

    private ChatMessageAttachment storeImageAttachment(UUID messageId, UUID currentAccountId, MultipartFile file) {
        StoredFile storedFile = fileStoragePort.store(new StoreFileCommand(
                file,
                StorageBucket.PRIVATE,
                StorageFolder.CHAT_ATTACHMENT,
                "chat_message",
                messageId,
                currentAccountId,
                Map.of("attachment_type", ChatAttachmentType.IMAGE.name())
        ));

        ChatMessageAttachment attachment = new ChatMessageAttachment();
        attachment.setAttachmentId(UUID.randomUUID());
        attachment.setMessageId(messageId);
        attachment.setBucket(StorageBucket.PRIVATE);
        attachment.setObjectKey(storedFile.objectKey());
        attachment.setOriginalFilename(storedFile.originalFilename());
        attachment.setContentType(storedFile.contentType());
        attachment.setSizeBytes(storedFile.sizeBytes());
        attachment.setChecksumSha256(storedFile.checksumSha256());
        attachment.setAttachmentType(ChatAttachmentType.IMAGE);
        return attachment;
    }

    private UUID resolveReplyMessageId(UUID conversationId, UUID replyToMessageId) {
        if (replyToMessageId == null) {
            return null;
        }
        ChatMessage replyMessage = chatPortOut.findMessageById(replyToMessageId)
                .orElseThrow(() -> new NotFoundException("Reply message not found"));
        if (!conversationId.equals(replyMessage.getConversationId())) {
            throw new BadRequestException("replyToMessageId does not belong to conversation");
        }
        return replyToMessageId;
    }

    private UUID resolveCustomerIdForSupportConversation(CurrentAccountAccess currentAccount, UUID requestedCustomerId) {
        if (requestedCustomerId != null && currentAccountPortIn.hasPermission("CHAT_CONVERSATION_CREATE_ALL")) {
            if (!chatPortOut.existsCustomer(requestedCustomerId)) {
                throw new NotFoundException("Customer not found");
            }
            return requestedCustomerId;
        }
        return chatPortOut.findCustomerIdByAccountId(currentAccount.accountId())
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));
    }

    private void requireSendAccess(UUID conversationId) {
        currentAccountPortIn.requirePermission("CHAT_MESSAGE_SEND_OWN");
        ChatConversation conversation = getConversationOrThrow(conversationId);
        conversationPolicy.ensureCanReceiveUserMessage(conversation);
        requireActiveMember(conversationId, requireCurrentAccountId());
    }

    private void requireReadAccess(ChatConversation conversation) {
        if (currentAccountPortIn.hasPermission("CHAT_CONVERSATION_READ_ALL")) {
            return;
        }
        currentAccountPortIn.requirePermission("CHAT_CONVERSATION_READ_OWN");
        requireActiveMember(conversation.getConversationId(), requireCurrentAccountId());
    }

    private void requireManageMembersAccess(ChatConversation conversation) {
        UUID currentAccountId = requireCurrentAccountId();
        if (currentAccountPortIn.hasPermission("CHAT_CONVERSATION_UPDATE_ALL")) {
            return;
        }
        ChatConversationMember member = chatPortOut.findMember(conversation.getConversationId(), currentAccountId)
                .orElseThrow(() -> new AccessDeniedException("Access is denied"));
        if (member.getStatus() != ChatMemberStatus.ACTIVE || member.getMemberRole() != ChatMemberRole.OWNER) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private void requireActiveMember(UUID conversationId, UUID accountId) {
        if (!chatPortOut.existsActiveMember(conversationId, accountId)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private ChatConversation getConversationOrThrow(UUID conversationId) {
        if (conversationId == null) {
            throw new BadRequestException("conversationId must not be null");
        }
        return chatPortOut.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    private ChatMessage resolveLastMessage(ChatConversation conversation) {
        if (conversation.getLastMessageId() == null) {
            return null;
        }
        return chatPortOut.findMessageById(conversation.getLastMessageId()).orElse(null);
    }

    private UUID requireCurrentAccountId() {
        return currentAccountPortIn.getCurrentAccountIdOrThrow();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit > MAX_HISTORY_LIMIT) {
            return MAX_HISTORY_LIMIT;
        }
        return limit;
    }
}
