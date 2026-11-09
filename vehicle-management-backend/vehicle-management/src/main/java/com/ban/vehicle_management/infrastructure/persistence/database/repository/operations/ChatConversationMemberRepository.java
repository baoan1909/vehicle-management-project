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

    @Modifying
    @Query(value = """
        update operations.chat_conversation_members set status='REMOVED', left_at=:now, updated_at=:now
        where account_id=:accountId and status='ACTIVE'
        """, nativeQuery = true)
    int removeAllForDeletedAccount(@Param("accountId") UUID accountId, @Param("now") java.time.Instant now);

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
                   CASE WHEN account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN 'Tài khoản đã xóa' ELSE account.username END AS username,
                   CASE WHEN account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN NULL ELSE account.email END AS email,
                   role.code AS accountRoleCode,
                   role.name AS accountRoleName,
                   CASE WHEN account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN 'Tài khoản đã xóa' ELSE profile.fullName END AS fullName,
                   CASE WHEN account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN NULL ELSE avatar.objectKey END AS avatarObjectKey
            FROM ChatConversationMemberEntity member
            JOIN AccountEntity account
                ON account.accountId = member.accountId
            JOIN account.role role
            LEFT JOIN UserProfileEntity profile
                ON profile.userProfileId = account.userProfileId
            LEFT JOIN UserProfileAvatarEntity avatar
                ON avatar.userProfileId = account.userProfileId
               AND avatar.current = TRUE
            WHERE member.conversationId IN :conversationIds
              AND account.status <> com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION
              AND (
                    member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
                    OR account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
              )
            ORDER BY member.joinedAt ASC, member.conversationMemberId ASC
            """)
    List<ChatConversationParticipantProjection> findActiveParticipantsByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds
    );

    @Query("""
            SELECT member.accountId
            FROM ChatConversationMemberEntity member
            JOIN AccountEntity account ON account.accountId = member.accountId
            WHERE member.conversationId = :conversationId
              AND member.status = com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus.ACTIVE
              AND account.status = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.ACTIVE
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
