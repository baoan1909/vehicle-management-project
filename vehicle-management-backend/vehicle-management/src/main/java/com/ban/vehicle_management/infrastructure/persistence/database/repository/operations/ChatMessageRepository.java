package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatMessageEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    @Query("""
            SELECT message
            FROM ChatMessageEntity message
            WHERE message.conversationId = :conversationId
              AND (:beforeCreatedAt IS NULL OR message.createdAt < :beforeCreatedAt)
            ORDER BY message.createdAt DESC, message.messageId DESC
            """)
    List<ChatMessageEntity> findHistory(
            @Param("conversationId") UUID conversationId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(message)
            FROM ChatMessageEntity message
            JOIN ChatConversationMemberEntity member
                ON member.conversationId = message.conversationId
               AND member.accountId = :accountId
            LEFT JOIN ChatMessageEntity lastReadMessage
                ON lastReadMessage.messageId = member.lastReadMessageId
            WHERE message.conversationId = :conversationId
              AND message.deleted = FALSE
              AND (message.senderAccountId IS NULL OR message.senderAccountId <> :accountId)
              AND (member.lastReadMessageId IS NULL OR message.createdAt > lastReadMessage.createdAt)
            """)
    long countUnreadMessages(
            @Param("conversationId") UUID conversationId,
            @Param("accountId") UUID accountId
    );
}
