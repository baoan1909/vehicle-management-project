package com.ban.vehicle_management.application.people.userprofile.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.AvatarApprovalPolicyPortOut;
import com.ban.vehicle_management.application.people.userprofile.model.AvatarModerationResult;
import com.ban.vehicle_management.application.people.userprofile.port.in.AvatarModerationPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.AvatarModerationPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfileAvatarPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvatarModerationUseCaseImpl implements AvatarModerationPortIn {
    public static final String REQUEST_TYPE = "USER_PROFILE_AVATAR_APPROVAL";
    public static final String TARGET_SCHEMA = "people";
    public static final String TARGET_TABLE = "user_profile_avatars";
    private static final String READ_ALL = "USER_PROFILE_READ_ALL";
    private static final String UPDATE_ALL = "USER_PROFILE_UPDATE_ALL";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarModerationUseCaseImpl.class);

    private final CurrentAccountPortIn currentAccountPortIn;
    private final SystemAccountIdPortOut systemAccountIdPortOut;
    private final AvatarModerationPortOut moderationPortOut;
    private final UserProfileAvatarPortOut avatarPortOut;
    private final AvatarApprovalPolicyPortOut policyPortOut;
    private final RequiresNewNotificationSender notificationSender;
    private final StorageUrlResolver storageUrlResolver;
    private final FileStoragePort fileStoragePort;
    private final ApprovalRequestPolicy approvalPolicy = new ApprovalRequestPolicy();

    public AvatarModerationUseCaseImpl(CurrentAccountPortIn currentAccountPortIn,
                                       SystemAccountIdPortOut systemAccountIdPortOut,
                                       AvatarModerationPortOut moderationPortOut,
                                       UserProfileAvatarPortOut avatarPortOut,
                                       AvatarApprovalPolicyPortOut policyPortOut,
                                       RequiresNewNotificationSender notificationSender,
                                       StorageUrlResolver storageUrlResolver,
                                       FileStoragePort fileStoragePort) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.systemAccountIdPortOut = systemAccountIdPortOut;
        this.moderationPortOut = moderationPortOut;
        this.avatarPortOut = avatarPortOut;
        this.policyPortOut = policyPortOut;
        this.notificationSender = notificationSender;
        this.storageUrlResolver = storageUrlResolver;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional
    public AvatarModerationResult submitCandidate(UserProfileAvatar avatar, UUID uploaderAccountId) {
        UUID ownerAccountId = moderationPortOut.findOwnerAccountId(avatar.getUserProfileId());
        ApprovalRequest request = new ApprovalRequest();
        request.setApprovalRequestId(UUID.randomUUID());
        request.setRequestType(REQUEST_TYPE);
        request.setTargetSchema(TARGET_SCHEMA);
        request.setTargetTable(TARGET_TABLE);
        request.setTargetId(avatar.getAvatarId());
        request.setRequestedBy(ownerAccountId);
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setRequestData(Map.of("uploaderAccountId", uploaderAccountId.toString()));
        approvalPolicy.initialize(request);
        request = moderationPortOut.save(request);

        var policy = policyPortOut.findPolicy();
        if (policy.isPresent() && policy.get().isEligible(request.getCreatedAt())) {
            return approveInternal(request, avatar, systemAccountIdPortOut.getSystemAccountId(),
                    "Automatically approved by avatar policy", "AUTO");
        }
        scheduleNotification(ownerAccountId, NotificationType.AVATAR_APPROVAL_SUBMITTED,
                "Ảnh đại diện đang chờ duyệt", "Ảnh đại diện mới của bạn đã được gửi xét duyệt.", avatar.getAvatarId());
        return toResult(request, avatar, ownerAccountId);
    }

    @Override
    @Transactional
    public void cancelPendingCandidate(UUID userProfileId) {
        avatarPortOut.findPendingByUserProfileId(userProfileId).ifPresent(avatar -> {
            moderationPortOut.findLatestByAvatarId(avatar.getAvatarId()).ifPresent(request -> {
                if (request.getStatus() == ApprovalRequestStatus.PENDING) {
                    approvalPolicy.cancel(request, "Replaced by a newer avatar candidate");
                    moderationPortOut.save(request);
                }
            });
            avatar.setStatus(UserProfileAvatarStatus.CANCELLED);
            avatar.setCurrent(false);
            avatarPortOut.save(avatar);
            deleteAfterCommit(avatar.getObjectKey());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AvatarModerationResult> getMyAvatarModerationStatus() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        return moderationPortOut.findLatestByOwnerAccountId(accountId).flatMap(request ->
                avatarPortOut.findById(request.getTargetId()).map(avatar -> toResult(request, avatar, accountId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvatarModerationResult> getAvatarApprovals(ApprovalRequestStatus status) {
        currentAccountPortIn.requirePermission(READ_ALL);
        return moderationPortOut.findAll(status).stream().map(request -> {
            UserProfileAvatar avatar = avatarPortOut.findById(request.getTargetId())
                    .orElseThrow(() -> new NotFoundException("Avatar candidate not found"));
            return toResult(request, avatar, request.getRequestedBy());
        }).toList();
    }

    @Override
    @Transactional
    public AvatarModerationResult approve(UUID approvalRequestId, String note) {
        currentAccountPortIn.requirePermission(UPDATE_ALL);
        return review(approvalRequestId, note, true);
    }

    @Override
    @Transactional
    public AvatarModerationResult reject(UUID approvalRequestId, String note) {
        currentAccountPortIn.requirePermission(UPDATE_ALL);
        return review(approvalRequestId, note, false);
    }

    private AvatarModerationResult review(UUID approvalRequestId, String note, boolean approve) {
        ApprovalRequest request = moderationPortOut.findForUpdate(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Avatar approval request not found"));
        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Avatar approval request has already been reviewed");
        }
        UserProfileAvatar avatar = avatarPortOut.findByIdForUpdate(request.getTargetId())
                .orElseThrow(() -> new NotFoundException("Avatar candidate not found"));
        if (avatar.getStatus() != UserProfileAvatarStatus.PENDING) {
            throw new ConflictException("Avatar candidate is no longer pending");
        }
        if (approve) {
            return approveInternal(request, avatar, currentAccountPortIn.getCurrentAccountIdOrThrow(),
                    normalizeNote(note), "MANUAL");
        }
        approvalPolicy.reject(request, normalizeNote(note));
        avatar.setStatus(UserProfileAvatarStatus.REJECTED);
        avatar.setCurrent(false);
        avatarPortOut.save(avatar);
        moderationPortOut.save(request);
        deleteAfterCommit(avatar.getObjectKey());
        scheduleNotification(request.getRequestedBy(), NotificationType.AVATAR_REJECTED,
                "Ảnh đại diện chưa được duyệt", "Vui lòng xem lý do từ chối và tải ảnh khác.", avatar.getAvatarId());
        return toResult(request, avatar, request.getRequestedBy());
    }

    private AvatarModerationResult approveInternal(ApprovalRequest request, UserProfileAvatar avatar,
                                                    UUID actorAccountId, String note, String mode) {
        Optional<UserProfileAvatar> previous = avatarPortOut.findCurrentByUserProfileId(avatar.getUserProfileId());
        avatarPortOut.markCurrentAsReplaced(avatar.getUserProfileId());
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        avatarPortOut.save(avatar);
        approvalPolicy.approve(request, actorAccountId, Instant.now(), note);
        request.setDecisionData(Map.of("decisionMode", mode));
        moderationPortOut.save(request);
        previous.map(UserProfileAvatar::getObjectKey).ifPresent(this::deleteAfterCommit);
        scheduleNotification(request.getRequestedBy(), NotificationType.AVATAR_APPROVED,
                "Ảnh đại diện đã được duyệt", "Ảnh đại diện mới của bạn đang được hiển thị.", avatar.getAvatarId());
        return toResult(request, avatar, request.getRequestedBy());
    }

    private AvatarModerationResult toResult(ApprovalRequest request, UserProfileAvatar candidate, UUID ownerAccountId) {
        String displayed = avatarPortOut.findCurrentByUserProfileId(candidate.getUserProfileId())
                .map(UserProfileAvatar::getObjectKey).map(storageUrlResolver::resolvePublicAvatarUrl).orElse(null);
        String preview = candidate.getStatus() == UserProfileAvatarStatus.PENDING
                ? storageUrlResolver.resolvePublicAvatarUrl(candidate.getObjectKey()) : null;
        return new AvatarModerationResult(candidate.getAvatarId(), request.getApprovalRequestId(),
                candidate.getUserProfileId(), ownerAccountId, request.getStatus(), request.getNote(), displayed,
                preview, request.getCreatedAt());
    }

    private String normalizeNote(String note) {
        return TextValidationUtils.normalizeNullableText(note, "note", 1000);
    }

    private void scheduleNotification(UUID accountId, NotificationType type, String title, String message, UUID avatarId) {
        TransactionalEvents.runAfterCommit(() -> {
            try {
                notificationSender.send(new SendNotificationCommand(accountId, type, title, message,
                        "/", TARGET_SCHEMA, TARGET_TABLE, avatarId));
            } catch (RuntimeException exception) {
                LOGGER.warn("Avatar workflow completed but notification failed for account {}", accountId, exception);
            }
        });
    }

    private void deleteAfterCommit(String objectKey) {
        TransactionalEvents.runAfterCommit(() -> {
            try { fileStoragePort.delete(objectKey); }
            catch (RuntimeException exception) { LOGGER.warn("Could not delete avatar object {}", objectKey, exception); }
        });
    }
}
