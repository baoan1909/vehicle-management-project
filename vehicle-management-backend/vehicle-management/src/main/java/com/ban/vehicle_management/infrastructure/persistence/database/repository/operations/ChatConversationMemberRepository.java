package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ChatConversationMemberEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.operations.ChatConversationParticipantProjection;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatConversationMemberRepository extends JpaRepository<ChatConversationMemberEntity, UUID> {

    Optional<ChatConversationMemberEntity> findByConversationIdAndAccountId(UUID conversationId, UUID accountId);

    boolean existsByConversationIdAndAccountIdAndStatus(
            UUID conversationId,
            UUID accountId,
            ChatMemberStatus status
    );

    List<ChatConversationMemberEntity> findByConversationIdAndStatus(UUID conversationId, ChatMemberStatus status);

    @Query("""
            SELECT member.conversationId AS conversationId,
                   member.accountId AS accountId,
                   member.memberRole AS memberRole,
                   account.username AS username,
                   account.email AS email,
                   profile.fullName AS fullName,
                   avatar.objectKey AS avatarObjectKey
            FROM ChatConversationMemberEntity member
            JOIN AccountEntity account
                ON account.accountId = member.accountId
            LEFT JOIN UserProfileEntity profile
                ON profile.userProfileId = account.userProfileId
            LEFT JOIN UserProfileAvatarEntity avatar
                ON avatar.userProfileId = account.userProfileId
               AND avatar.current = TRUE
            WHERE member.conversationId IN :conversationIds
              AND member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
            ORDER BY member.joinedAt ASC, member.conversationMemberId ASC
            """)
    List<ChatConversationParticipantProjection> findActiveParticipantsByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds
    );

    @Query("""
            SELECT member.accountId
            FROM ChatConversationMemberEntity member
            WHERE member.conversationId = :conversationId
              AND member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
            """)
    List<UUID> findActiveMemberAccountIds(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("""
            UPDATE ChatConversationMemberEntity member
            SET member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.REMOVED,
                member.leftAt = :leftAt
            WHERE member.conversationId = :conversationId
              AND member.accountId = :accountId
            """)
    void removeMember(
            @Param("conversationId") UUID conversationId,
            @Param("accountId") UUID accountId,
            @Param("leftAt") java.time.Instant leftAt
    );

    @Modifying
    @Query("""
            UPDATE ChatConversationMemberEntity member
            SET member.lastReadMessageId = :messageId
            WHERE member.conversationId = :conversationId
              AND member.accountId = :accountId
            """)
    void markRead(
            @Param("conversationId") UUID conversationId,
            @Param("accountId") UUID accountId,
            @Param("messageId") UUID messageId
    );
}
