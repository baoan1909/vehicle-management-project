package com.ban.vehicle_management.application.notification.broadcastannouncement.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.broadcastannouncement.mapper.BroadcastAnnouncementDomainMapper;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.in.BroadcastAnnouncementPortIn;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.policy.BroadcastAnnouncementPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BroadcastAnnouncementUseCaseImpl implements BroadcastAnnouncementPortIn {

    private static final String GUEST_ROLE_CODE = "GUEST";
    private static final String BROADCAST_NOTIFICATION_CREATE_ALL = "BROADCAST_NOTIFICATION_CREATE_ALL";
    private static final String BROADCAST_NOTIFICATION_READ_ALL = "BROADCAST_NOTIFICATION_READ_ALL";
    private static final String BROADCAST_NOTIFICATION_UPDATE_ALL = "BROADCAST_NOTIFICATION_UPDATE_ALL";
    private static final String BROADCAST_NOTIFICATION_DELETE_ALL = "BROADCAST_NOTIFICATION_DELETE_ALL";
    private static final String BROADCAST_NOTIFICATION_PUBLISH_ALL = "BROADCAST_NOTIFICATION_PUBLISH_ALL";
    private static final String BROADCAST_NOTIFICATION_CANCEL_ALL = "BROADCAST_NOTIFICATION_CANCEL_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final BroadcastAnnouncementPortOut broadcastAnnouncementPortOut;
    private final NotificationPortIn notificationPortIn;
    private final BroadcastAnnouncementDomainMapper broadcastAnnouncementDomainMapper;
    private final BroadcastAnnouncementPolicy broadcastAnnouncementPolicy = new BroadcastAnnouncementPolicy();

    public BroadcastAnnouncementUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            BroadcastAnnouncementPortOut broadcastAnnouncementPortOut,
            NotificationPortIn notificationPortIn,
            BroadcastAnnouncementDomainMapper broadcastAnnouncementDomainMapper
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.broadcastAnnouncementPortOut = broadcastAnnouncementPortOut;
        this.notificationPortIn = notificationPortIn;
        this.broadcastAnnouncementDomainMapper = broadcastAnnouncementDomainMapper;
    }

    @Override
    @Transactional
    public BroadcastAnnouncement createBroadcastAnnouncement(BroadcastAnnouncement announcement) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_CREATE_ALL);
        broadcastAnnouncementPolicy.initializeNew(announcement);
        ensureTitleUnique(announcement.getTitle(), announcement.getBroadcastId());
        return broadcastAnnouncementPortOut.save(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public BroadcastAnnouncement getBroadcastAnnouncementById(UUID broadcastId) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_READ_ALL);
        return findExistingBroadcastAnnouncement(broadcastId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BroadcastAnnouncement> getBroadcastAnnouncements(BroadcastAnnouncementStatus status) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_READ_ALL);
        return broadcastAnnouncementPortOut.findAll(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BroadcastAnnouncement> getActiveBroadcastAnnouncementsForCurrentAccount() {
        String roleCode = currentAccountPortIn.getCurrentAccount()
                .map(CurrentAccountAccess::roleCode)
                .filter(role -> role != null && !role.isBlank())
                .orElse(GUEST_ROLE_CODE);

        return broadcastAnnouncementPortOut.findActivePublishedForRole(
                roleCode,
                Instant.now()
        );
    }

    @Override
    @Transactional
    public BroadcastAnnouncement updateBroadcastAnnouncement(UUID broadcastId, BroadcastAnnouncement request) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_UPDATE_ALL);
        BroadcastAnnouncement existing = findExistingBroadcastAnnouncement(broadcastId);

        broadcastAnnouncementDomainMapper.updateEditableFields(request, existing);
        broadcastAnnouncementPolicy.validateForUpdate(existing);
        ensureTitleUnique(existing.getTitle(), existing.getBroadcastId());
        return broadcastAnnouncementPortOut.save(existing);
    }

    @Override
    @Transactional
    public BroadcastAnnouncement updateBroadcastAnnouncementDisplayOrder(UUID broadcastId, Integer displayOrder) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_UPDATE_ALL);
        BroadcastAnnouncement existing = findExistingBroadcastAnnouncement(broadcastId);

        broadcastAnnouncementPolicy.updateDisplayOrder(existing, displayOrder);
        return broadcastAnnouncementPortOut.save(existing);
    }

    @Override
    @Transactional
    public BroadcastAnnouncement publishBroadcastAnnouncement(UUID broadcastId) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_PUBLISH_ALL);
        BroadcastAnnouncement existing = findExistingBroadcastAnnouncement(broadcastId);

        broadcastAnnouncementPolicy.publish(existing, Instant.now());
        ensureTitleUnique(existing.getTitle(), existing.getBroadcastId());
        notificationPortIn.sendBroadcastWebNotification(
                broadcastAnnouncementDomainMapper.toBroadcastNotificationCommand(
                        existing,
                        existing.getAudienceType() == BroadcastAnnouncementAudienceType.ALL_ACTIVE_ACCOUNTS
                )
        );
        return broadcastAnnouncementPortOut.save(existing);
    }

    @Override
    @Transactional
    public BroadcastAnnouncement cancelBroadcastAnnouncement(UUID broadcastId) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_CANCEL_ALL);
        BroadcastAnnouncement existing = findExistingBroadcastAnnouncement(broadcastId);

        broadcastAnnouncementPolicy.cancel(existing, Instant.now());
        return broadcastAnnouncementPortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteBroadcastAnnouncement(UUID broadcastId) {
        currentAccountPortIn.requirePermission(BROADCAST_NOTIFICATION_DELETE_ALL);
        BroadcastAnnouncement existing = findExistingBroadcastAnnouncement(broadcastId);
        broadcastAnnouncementPolicy.ensureDeletable(existing);
        broadcastAnnouncementPortOut.deleteById(broadcastId);
    }

    private BroadcastAnnouncement findExistingBroadcastAnnouncement(UUID broadcastId) {
        return broadcastAnnouncementPortOut.findById(broadcastId)
                .orElseThrow(() -> new NotFoundException("Broadcast announcement not found"));
    }

    private void ensureTitleUnique(String title, UUID broadcastId) {
        boolean titleExists = broadcastId == null
                ? broadcastAnnouncementPortOut.existsByTitle(title)
                : broadcastAnnouncementPortOut.existsByTitleAndBroadcastIdNot(title, broadcastId);
        if (titleExists) {
            throw new ConflictException("Tiêu đề thông báo đã tồn tại.");
        }
    }

}
