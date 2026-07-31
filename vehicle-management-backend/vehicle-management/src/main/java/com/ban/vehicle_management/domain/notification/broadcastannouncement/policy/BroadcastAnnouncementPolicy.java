package com.ban.vehicle_management.domain.notification.broadcastannouncement.policy;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class BroadcastAnnouncementPolicy {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int REDIRECT_URL_MAX_LENGTH = 1000;
    private static final int RELATED_SCHEMA_MAX_LENGTH = 50;
    private static final int RELATED_TABLE_MAX_LENGTH = 80;
    private static final int ROLE_CODE_MAX_LENGTH = 50;

    public void initializeNew(BroadcastAnnouncement announcement) {
        requireAnnouncement(announcement);
        announcement.setBroadcastId(UUID.randomUUID());
        announcement.setStatus(BroadcastAnnouncementStatus.DRAFT);
        announcement.setPublishedAt(null);
        announcement.setCancelledAt(null);
        normalizeEditableFields(announcement);
    }

    public void validateForUpdate(BroadcastAnnouncement announcement) {
        requireAnnouncement(announcement);
        if (announcement.getStatus() == BroadcastAnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Cannot update a published broadcast announcement");
        }
        if (announcement.getStatus() == BroadcastAnnouncementStatus.CANCELLED) {
            throw new ConflictException("Cannot update a cancelled broadcast announcement");
        }
        normalizeEditableFields(announcement);
    }

    public void publish(BroadcastAnnouncement announcement, Instant publishedAt) {
        requireAnnouncement(announcement);
        if (announcement.getStatus() == BroadcastAnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Broadcast announcement was already published");
        }
        if (announcement.getStatus() == BroadcastAnnouncementStatus.CANCELLED) {
            throw new ConflictException("Cannot publish a cancelled broadcast announcement");
        }
        if (!Boolean.TRUE.equals(announcement.getEnabled())) {
            throw new ConflictException("Cannot publish a disabled broadcast announcement");
        }
        normalizeEditableFields(announcement);
        announcement.setStatus(BroadcastAnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(publishedAt);
        announcement.setCancelledAt(null);
    }

    public void cancel(BroadcastAnnouncement announcement, Instant cancelledAt) {
        requireAnnouncement(announcement);
        if (announcement.getStatus() == BroadcastAnnouncementStatus.CANCELLED) {
            return;
        }
        if (announcement.getStatus() == BroadcastAnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Cannot cancel a published broadcast announcement");
        }
        announcement.setStatus(BroadcastAnnouncementStatus.CANCELLED);
        announcement.setCancelledAt(cancelledAt);
        announcement.setEnabled(false);
    }

    public void ensureDeletable(BroadcastAnnouncement announcement) {
        requireAnnouncement(announcement);
        if (announcement.getStatus() == BroadcastAnnouncementStatus.PUBLISHED) {
            throw new ConflictException("Cannot delete a published broadcast announcement");
        }
    }

    private void normalizeEditableFields(BroadcastAnnouncement announcement) {
        if (announcement.getNotificationType() == null) {
            throw new BadRequestException("notificationType must not be null");
        }
        if (announcement.getAudienceType() == null) {
            throw new BadRequestException("audienceType must not be null");
        }
        if (announcement.getStartAt() == null) {
            throw new BadRequestException("startAt must not be null");
        }
        if (announcement.getEndAt() != null && announcement.getEndAt().isBefore(announcement.getStartAt())) {
            throw new BadRequestException("endAt must be after or equal to startAt");
        }

        announcement.setTitle(TextValidationUtils.normalizeRequiredText(
                announcement.getTitle(),
                "title",
                TITLE_MAX_LENGTH
        ));
        announcement.setMessage(TextValidationUtils.normalizeRequiredText(
                announcement.getMessage(),
                "message",
                0
        ));
        announcement.setRedirectUrl(TextValidationUtils.normalizeNullableText(
                announcement.getRedirectUrl(),
                "redirectUrl",
                REDIRECT_URL_MAX_LENGTH
        ));
        announcement.setRelatedSchema(TextValidationUtils.normalizeNullableText(
                announcement.getRelatedSchema(),
                "relatedSchema",
                RELATED_SCHEMA_MAX_LENGTH
        ));
        announcement.setRelatedTable(TextValidationUtils.normalizeNullableText(
                announcement.getRelatedTable(),
                "relatedTable",
                RELATED_TABLE_MAX_LENGTH
        ));
        announcement.setRoleCodes(normalizeRoleCodes(
                announcement.getAudienceType(),
                announcement.getRoleCodes()
        ));
        if (announcement.getEnabled() == null) {
            announcement.setEnabled(true);
        }
    }

    private Set<String> normalizeRoleCodes(
            BroadcastAnnouncementAudienceType audienceType,
            Set<String> roleCodes
    ) {
        if (audienceType == BroadcastAnnouncementAudienceType.ALL_ACTIVE_ACCOUNTS) {
            return Set.of();
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BadRequestException("roleCodes must not be empty when audienceType is ROLE_CODES");
        }

        Set<String> normalizedRoleCodes = new LinkedHashSet<>();
        roleCodes.forEach(roleCode -> normalizedRoleCodes.add(
                TextValidationUtils.normalizeCode(roleCode, "roleCode", ROLE_CODE_MAX_LENGTH)
        ));
        return normalizedRoleCodes;
    }

    private void requireAnnouncement(BroadcastAnnouncement announcement) {
        if (announcement == null) {
            throw new BadRequestException("broadcast announcement must not be null");
        }
    }
}
