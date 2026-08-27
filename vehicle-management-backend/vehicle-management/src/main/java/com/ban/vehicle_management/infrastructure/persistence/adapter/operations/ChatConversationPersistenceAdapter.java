package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationParticipant;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessageAttachment;
import com.ban.vehicle_management.infrastructure.mapper.operations.ChatConversationMemberPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.operations.ChatConversationParticipantPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.operations.ChatConversationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.operations.ChatMessageAttachmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.operations.ChatMessagePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationMemberEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageAttachmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationMemberRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatMessageAttachmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatMessageRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ChatConversationPersistenceAdapter implements ChatConversationPortOut {

    private final ChatConversationRepository conversationRepository;
    private final ChatConversationMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageAttachmentRepository attachmentRepository;
    private final AccountRepository accountRepository;
    private final AccountAuthorizationPortOut accountAuthorizationPortOut;
    private final CustomerRepository customerRepository;
    private final ChatConversationPersistenceMapper conversationMapper;
    private final ChatConversationMemberPersistenceMapper memberMapper;
    private final ChatConversationParticipantPersistenceMapper participantMapper;
    private final ChatMessagePersistenceMapper messageMapper;
    private final ChatMessageAttachmentPersistenceMapper attachmentMapper;

    public ChatConversationPersistenceAdapter(
            ChatConversationRepository conversationRepository,
            ChatConversationMemberRepository memberRepository,
            ChatMessageRepository messageRepository,
            ChatMessageAttachmentRepository attachmentRepository,
            AccountRepository accountRepository,
            AccountAuthorizationPortOut accountAuthorizationPortOut,
            CustomerRepository customerRepository,
            ChatConversationPersistenceMapper conversationMapper,
            ChatConversationMemberPersistenceMapper memberMapper,
            ChatConversationParticipantPersistenceMapper participantMapper,
            ChatMessagePersistenceMapper messageMapper,
            ChatMessageAttachmentPersistenceMapper attachmentMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.accountRepository = accountRepository;
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
        this.customerRepository = customerRepository;
        this.conversationMapper = conversationMapper;
        this.memberMapper = memberMapper;
        this.participantMapper = participantMapper;
        this.messageMapper = messageMapper;
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public ChatConversation saveConversation(ChatConversation conversation) {
        ChatConversationEntity savedEntity = conversationRepository.saveAndFlush(conversationMapper.toEntity(conversation));
        return conversationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChatConversation> findConversationById(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .map(conversationMapper::toDomain)
                .map(this::attachParticipants);
    }

    @Override
    public List<ChatConversation> findInboxConversations(UUID accountId) {
        List<ChatConversation> conversations = conversationRepository.findInboxConversations(accountId).stream()
                .map(conversationMapper::toDomain)
                .toList();
        attachParticipants(conversations);
        return conversations;
    }

    @Override
    public Optional<ChatConversation> findInternalDirectConversation(UUID firstAccountId, UUID secondAccountId) {
        return conversationRepository.findDirectConversation(
                        ChatConversationType.INTERNAL_DIRECT,
                        firstAccountId,
                        secondAccountId
                )
                .map(conversationMapper::toDomain)
                .map(this::attachParticipants);
    }

    @Override
    public Optional<ChatConversation> findActiveCustomerSupportConversation(UUID customerId, UUID staffAccountId) {
        return conversationRepository.findDirectConversation(
                        ChatConversationType.CUSTOMER_DIRECT,
                        findAccountIdByCustomerId(customerId).orElse(null),
                        staffAccountId
                )
                .map(conversationMapper::toDomain)
                .map(this::attachParticipants);
    }

    @Override
    public Optional<ChatConversation> findActiveSupportTicketConversation(UUID supportTicketId) {
        return conversationRepository.findFirstBySupportTicketIdAndConversationTypeAndStatus(
                        supportTicketId,
                        ChatConversationType.SUPPORT_TICKET,
                        ChatConversationStatus.ACTIVE
                )
                .map(conversationMapper::toDomain)
                .map(this::attachParticipants);
    }

    @Override
    public List<ChatConversation> findActiveSupportTicketConversations() {
        List<ChatConversation> conversations = conversationRepository
                .findByConversationTypeAndStatusOrderByCreatedAtDesc(
                        ChatConversationType.SUPPORT_TICKET,
                        ChatConversationStatus.ACTIVE
                )
                .stream()
                .map(conversationMapper::toDomain)
                .toList();
        attachParticipants(conversations);
        return conversations;
    }

    @Override
    public ChatConversationMember saveMember(ChatConversationMember member) {
        ChatConversationMemberEntity savedEntity = memberRepository.saveAndFlush(memberMapper.toEntity(member));
        return memberMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChatConversationMember> findMember(UUID conversationId, UUID accountId) {
        return memberRepository.findByConversationIdAndAccountId(conversationId, accountId)
                .map(memberMapper::toDomain);
    }

    @Override
    public List<ChatConversationMember> findActiveMembers(UUID conversationId) {
        return memberRepository.findByConversationIdAndStatus(conversationId, ChatMemberStatus.ACTIVE).stream()
                .map(memberMapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findActiveMemberAccountIds(UUID conversationId) {
        return memberRepository.findActiveMemberAccountIds(conversationId);
    }

    @Override
    public void removeMember(UUID conversationId, UUID accountId, Instant leftAt) {
        memberRepository.removeMember(conversationId, accountId, leftAt);
    }

    @Override
    public boolean existsActiveMember(UUID conversationId, UUID accountId) {
        return memberRepository.existsByConversationIdAndAccountIdAndStatus(
                conversationId,
                accountId,
                ChatMemberStatus.ACTIVE
        );
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        ChatMessageEntity savedEntity = messageRepository.saveAndFlush(messageMapper.toEntity(message));
        ChatMessage savedMessage = messageMapper.toDomain(savedEntity);
        savedMessage.setAttachments(message.getAttachments());
        return savedMessage;
    }

    @Override
    public Optional<ChatMessage> findMessageById(UUID messageId) {
        return messageRepository.findById(messageId).map(messageMapper::toDomain);
    }

    @Override
    public List<ChatMessage> findMessageHistory(UUID conversationId, Instant beforeCreatedAt, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<ChatMessageEntity> messageEntities = beforeCreatedAt == null
                ? messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDescMessageIdDesc(
                        conversationId,
                        pageRequest
                )
                : messageRepository.findByConversationIdAndDeletedFalseAndCreatedAtBeforeOrderByCreatedAtDescMessageIdDesc(
                        conversationId,
                        beforeCreatedAt,
                        pageRequest
                );
        List<ChatMessage> messages = messageEntities
                .stream()
                .map(messageMapper::toDomain)
                .toList();
        attachFiles(messages);
        return messages;
    }

    @Override
    public long countUnreadMessages(UUID conversationId, UUID accountId) {
        return messageRepository.countUnreadMessages(conversationId, accountId);
    }

    @Override
    public ChatMessageAttachment saveAttachment(ChatMessageAttachment attachment) {
        ChatMessageAttachmentEntity savedEntity = attachmentRepository.saveAndFlush(attachmentMapper.toEntity(attachment));
        return attachmentMapper.toDomain(savedEntity);
    }

    @Override
    public List<ChatMessageAttachment> findAttachmentsByMessageIds(Collection<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return attachmentRepository.findByMessageIdIn(messageIds).stream()
                .map(attachmentMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ChatMessageAttachment> findAttachmentById(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId).map(attachmentMapper::toDomain);
    }

    @Override
    public void markRead(UUID conversationId, UUID accountId, UUID messageId) {
        memberRepository.markRead(conversationId, accountId, messageId);
    }

    @Override
    public boolean existsActiveAccount(UUID accountId) {
        return accountAuthorizationPortOut.findByAccountId(accountId)
                .map(access -> access.canUseBusinessPermissions())
                .orElse(false);
    }

    @Override
    public boolean existsActiveAccountWithPermissions(UUID accountId, Set<String> requiredPermissionCodes) {
        if (requiredPermissionCodes == null || requiredPermissionCodes.isEmpty()) {
            return existsActiveAccount(accountId);
        }
        return accountAuthorizationPortOut.findByAccountId(accountId)
                .filter(access -> access.canUseBusinessPermissions())
                .map(access -> access.getEffectivePermissionCodes().containsAll(requiredPermissionCodes))
                .orElse(false);
    }

    @Override
    public Optional<UUID> findCustomerIdByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(account -> account.getUserProfileId())
                .flatMap(customerRepository::findByUserProfileId)
                .map(customer -> customer.getCustomerId());
    }

    @Override
    public boolean existsSupportTicketCard(UUID conversationId, UUID supportTicketId) {
        return messageRepository.existsByConversationIdAndRelatedSchemaAndRelatedTableAndRelatedIdAndMessageTypeAndDeletedFalse(
                conversationId,
                "operations",
                "support_tickets",
                supportTicketId,
                ChatMessageType.SUPPORT_REQUEST
        );
    }

    @Override
    public Optional<UUID> findAccountIdByCustomerId(UUID customerId) {
        return customerRepository.findAccountIdByCustomerId(customerId);
    }

    @Override
    public boolean existsCustomer(UUID customerId) {
        return customerRepository.existsById(customerId);
    }

    private void attachFiles(List<ChatMessage> messages) {
        List<UUID> messageIds = messages.stream()
                .map(ChatMessage::getMessageId)
                .toList();
        Map<UUID, List<ChatMessageAttachment>> attachmentsByMessageId = findAttachmentsByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(ChatMessageAttachment::getMessageId));
        messages.forEach(message -> message.setAttachments(
                attachmentsByMessageId.getOrDefault(message.getMessageId(), List.of())
        ));
    }

    private ChatConversation attachParticipants(ChatConversation conversation) {
        attachParticipants(List.of(conversation));
        return conversation;
    }

    private void attachParticipants(List<ChatConversation> conversations) {
        List<UUID> conversationIds = conversations.stream()
                .map(ChatConversation::getConversationId)
                .toList();
        if (conversationIds.isEmpty()) {
            return;
        }

        Map<UUID, List<ChatConversationParticipant>> participantsByConversationId = memberRepository
                .findActiveParticipantsByConversationIds(conversationIds)
                .stream()
                .map(participantMapper::toDomain)
                .collect(Collectors.groupingBy(ChatConversationParticipant::getConversationId));

        conversations.forEach(conversation -> conversation.setParticipants(
                participantsByConversationId.getOrDefault(conversation.getConversationId(), List.of())
        ));
    }
}
