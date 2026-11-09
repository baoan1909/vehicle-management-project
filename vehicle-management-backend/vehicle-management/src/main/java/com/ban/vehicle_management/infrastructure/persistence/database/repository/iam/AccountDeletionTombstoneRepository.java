package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDeletionTombstoneRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountDeletionTombstoneRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAvatarObjectKeys(UUID profileId) {
        if (profileId == null) return List.of();
        return jdbcTemplate.queryForList("""
                select object_key from people.user_profile_avatars
                 where user_profile_id=? and object_key not like '_deleted_%'
                """, String.class, profileId);
    }

    public void tombstoneRelatedData(UUID accountId, UUID profileId, String marker, Instant now) {
        if (profileId != null) {
            jdbcTemplate.update("""
                    update people.customers set customer_code=?, status='DELETED', approved_by=null, updated_at=?
                     where user_profile_id=? and status<>'DELETED'
                    """, marker, now, profileId);
            jdbcTemplate.update("""
                    update people.employees set employee_code=?, job_title=null, hired_at=null,
                           status='DELETED', updated_at=? where user_profile_id=?
                    """, marker, now, profileId);
            jdbcTemplate.update("""
                    update people.user_profile_avatars set status='DELETED', is_current=false, updated_at=?
                     where user_profile_id=?
                    """, now, profileId);
            jdbcTemplate.update("""
                    update people.user_profiles set full_name=?, phone_number=?, identify_card=?,
                           address_detail=?, province_code=null, district_code=null, ward_code=null,
                           address_display=?, date_of_birth=null, gender=null, status='DELETED', updated_at=?
                     where user_profile_id=?
                    """, marker, marker, marker, marker, marker, now, profileId);
        }
        jdbcTemplate.update("""
                update operations.approval_requests set status='CANCELLED', updated_at=?
                 where status='PENDING'
                   and request_type in ('PARTNER_REGISTRATION','INTERNAL_EMPLOYEE_ONBOARDING',
                                        'CUSTOMER_ONBOARDING','SYSTEM_ADMIN_ONBOARDING')
                   and (requested_by=? or target_id=? or target_id=? or target_id in (
                       select employee_id from people.employees where user_profile_id=?))
                """, now, accountId, accountId, profileId, profileId);
        jdbcTemplate.update("""
                delete from iam.member_parking_lot_scopes scope
                 where scope.organization_membership_id in (
                    select membership.organization_membership_id from iam.organization_memberships membership
                     where membership.account_id=?)
                """, accountId);
        jdbcTemplate.update("""
                update iam.organization_memberships set status='INACTIVE', updated_at=?
                 where account_id=? and status='ACTIVE'
                """, now, accountId);
        jdbcTemplate.update("update iam.account_identities set provider_subject=?, updated_at=? where account_id=?",
                marker, now, accountId);
        jdbcTemplate.update("""
                update operations.chat_conversation_members set status='REMOVED', left_at=?, updated_at=?
                 where account_id=? and status='ACTIVE'
                """, now, now, accountId);
    }

    public void completeAvatarCleanup(UUID profileId, Instant now) {
        if (profileId == null) return;
        jdbcTemplate.update("""
                update people.user_profile_avatars
                   set object_key=concat('_deleted_', avatar_id), original_filename=null,
                       content_type=null, size_bytes=null, checksum_sha256=null, updated_at=?
                 where user_profile_id=?
                """, now, profileId);
    }
}
