package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {

    @Query("""
            SELECT conversation
            FROM ChatConversationEntity conversation
            JOIN ChatConversationMemberEntity member
                ON member.conversationId = conversation.conversationId
            WHERE member.accountId = :accountId
              AND member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
            ORDER BY
                CASE WHEN conversation.lastMessageAt IS NULL THEN 1 ELSE 0 END,
                conversation.lastMessageAt DESC,
                conversation.createdAt DESC
            """)
    List<ChatConversationEntity> findInboxConversations(@Param("accountId") UUID accountId);

    @Query("""
            SELECT conversation
            FROM ChatConversationEntity conversation
            WHERE conversation.conversationType = :conversationType
              AND conversation.status <> com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus.CLOSED
              AND EXISTS (
                    SELECT 1 FROM ChatConversationMemberEntity firstMember
                    WHERE firstMember.conversationId = conversation.conversationId
                      AND firstMember.accountId = :firstAccountId
                      AND firstMember.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
              )
              AND EXISTS (
                    SELECT 1 FROM ChatConversationMemberEntity secondMember
                    WHERE secondMember.conversationId = conversation.conversationId
                      AND secondMember.accountId = :secondAccountId
                      AND secondMember.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
              )
            """)
    Optional<ChatConversationEntity> findDirectConversation(
            @Param("conversationType") ChatConversationType conversationType,
            @Param("firstAccountId") UUID firstAccountId,
            @Param("secondAccountId") UUID secondAccountId
    );

    Optional<ChatConversationEntity> findFirstByCustomerIdAndConversationTypeAndStatus(
            UUID customerId,
            ChatConversationType conversationType,
            ChatConversationStatus status
    );

    Optional<ChatConversationEntity> findFirstBySupportTicketIdAndConversationTypeAndStatus(
            UUID supportTicketId,
            ChatConversationType conversationType,
            ChatConversationStatus status
    );

    List<ChatConversationEntity> findByConversationTypeAndStatusOrderByCreatedAtDesc(
            ChatConversationType conversationType,
            ChatConversationStatus status
    );
}
