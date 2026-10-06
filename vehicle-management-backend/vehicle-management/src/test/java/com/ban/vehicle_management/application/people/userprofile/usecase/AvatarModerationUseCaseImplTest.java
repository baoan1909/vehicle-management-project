package com.ban.vehicle_management.application.people.userprofile.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.AvatarApprovalPolicyPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.AvatarModerationPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfileAvatarPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvatarModerationUseCaseImplTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private SystemAccountIdPortOut systemAccountIdPortOut;
    @Mock private AvatarModerationPortOut moderationPortOut;
    @Mock private UserProfileAvatarPortOut avatarPortOut;
    @Mock private AvatarApprovalPolicyPortOut policyPortOut;
    @Mock private RequiresNewNotificationSender notificationSender;
    @Mock private StorageUrlResolver storageUrlResolver;
    @Mock private FileStoragePort fileStoragePort;

    private AvatarModerationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AvatarModerationUseCaseImpl(currentAccountPortIn, systemAccountIdPortOut,
                moderationPortOut, avatarPortOut, policyPortOut, notificationSender,
                storageUrlResolver, fileStoragePort);
    }

    @Test
    void shouldCreatePendingApprovalAndKeepApprovedAvatarVisible() {
        UUID ownerId = UUID.randomUUID();
        UserProfileAvatar candidate = avatar(UserProfileAvatarStatus.PENDING, false, "av/new-public.jpg");
        UserProfileAvatar current = avatar(UserProfileAvatarStatus.ACTIVE, true, "av/current-public.jpg");
        when(moderationPortOut.findOwnerAccountId(candidate.getUserProfileId())).thenReturn(ownerId);
        when(moderationPortOut.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest request = invocation.getArgument(0);
            request.setCreatedAt(Instant.parse("2026-10-05T08:00:00Z"));
            return request;
        });
        when(policyPortOut.findPolicy()).thenReturn(Optional.empty());
        when(avatarPortOut.findCurrentByUserProfileId(candidate.getUserProfileId())).thenReturn(Optional.of(current));
        when(storageUrlResolver.resolvePublicAvatarUrl(current.getObjectKey())).thenReturn("https://cdn/current.jpg");
        when(storageUrlResolver.resolvePublicAvatarUrl(candidate.getObjectKey())).thenReturn("https://cdn/new.jpg");

        var result = useCase.submitCandidate(candidate, ownerId);

        assertEquals(ApprovalRequestStatus.PENDING, result.approvalStatus());
        assertEquals("https://cdn/current.jpg", result.displayedAvatarUrl());
        assertEquals("https://cdn/new.jpg", result.candidatePreviewUrl());
        ArgumentCaptor<ApprovalRequest> requestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(moderationPortOut).save(requestCaptor.capture());
        assertEquals(AvatarModerationUseCaseImpl.REQUEST_TYPE, requestCaptor.getValue().getRequestType());
        assertEquals(candidate.getAvatarId(), requestCaptor.getValue().getTargetId());
        assertEquals(ownerId, requestCaptor.getValue().getRequestedBy());
        verify(avatarPortOut, never()).markCurrentAsReplaced(any(UUID.class));
        ArgumentCaptor<SendNotificationCommand> notificationCaptor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationSender).send(notificationCaptor.capture());
        assertEquals(NotificationType.AVATAR_APPROVAL_SUBMITTED, notificationCaptor.getValue().notificationType());
    }

    @Test
    void shouldApproveCandidateAndReplaceCurrentAvatarAtomically() {
        UUID reviewerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ApprovalRequest request = request(ownerId, ApprovalRequestStatus.PENDING);
        UserProfileAvatar candidate = avatar(UserProfileAvatarStatus.PENDING, false, "av/new-public.jpg");
        request.setTargetId(candidate.getAvatarId());
        UserProfileAvatar current = avatar(UserProfileAvatarStatus.ACTIVE, true, "av/current-public.jpg");
        when(moderationPortOut.findForUpdate(request.getApprovalRequestId())).thenReturn(Optional.of(request));
        when(avatarPortOut.findByIdForUpdate(candidate.getAvatarId())).thenReturn(Optional.of(candidate));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(reviewerId);
        when(avatarPortOut.findCurrentByUserProfileId(candidate.getUserProfileId()))
                .thenReturn(Optional.of(current), Optional.of(candidate));
        when(storageUrlResolver.resolvePublicAvatarUrl(candidate.getObjectKey())).thenReturn("https://cdn/new.jpg");
        when(moderationPortOut.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.approve(request.getApprovalRequestId(), "Hợp lệ");

        verify(currentAccountPortIn).requirePermission("USER_PROFILE_UPDATE_ALL");
        verify(avatarPortOut).markCurrentAsReplaced(candidate.getUserProfileId());
        assertEquals(UserProfileAvatarStatus.ACTIVE, candidate.getStatus());
        assertTrue(candidate.getCurrent());
        assertEquals(ApprovalRequestStatus.APPROVED, request.getStatus());
        assertEquals(reviewerId, request.getApprovedBy());
        assertEquals("MANUAL", request.getDecisionData().get("decisionMode"));
        assertEquals("https://cdn/new.jpg", result.displayedAvatarUrl());
        verify(fileStoragePort).delete(current.getObjectKey());
    }

    @Test
    void shouldRejectCandidateWithoutChangingCurrentAvatar() {
        UUID ownerId = UUID.randomUUID();
        ApprovalRequest request = request(ownerId, ApprovalRequestStatus.PENDING);
        UserProfileAvatar candidate = avatar(UserProfileAvatarStatus.PENDING, false, "av/new-public.jpg");
        request.setTargetId(candidate.getAvatarId());
        UserProfileAvatar current = avatar(UserProfileAvatarStatus.ACTIVE, true, "av/current-public.jpg");
        when(moderationPortOut.findForUpdate(request.getApprovalRequestId())).thenReturn(Optional.of(request));
        when(avatarPortOut.findByIdForUpdate(candidate.getAvatarId())).thenReturn(Optional.of(candidate));
        when(avatarPortOut.findCurrentByUserProfileId(candidate.getUserProfileId())).thenReturn(Optional.of(current));
        when(storageUrlResolver.resolvePublicAvatarUrl(current.getObjectKey())).thenReturn("https://cdn/current.jpg");
        when(moderationPortOut.save(any(ApprovalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.reject(request.getApprovalRequestId(), "Ảnh không rõ khuôn mặt");

        assertEquals(ApprovalRequestStatus.REJECTED, request.getStatus());
        assertEquals(UserProfileAvatarStatus.REJECTED, candidate.getStatus());
        assertFalse(candidate.getCurrent());
        assertEquals("https://cdn/current.jpg", result.displayedAvatarUrl());
        verify(avatarPortOut, never()).markCurrentAsReplaced(any(UUID.class));
        verify(fileStoragePort).delete(candidate.getObjectKey());
    }

    @Test
    void shouldRejectRepeatedReview() {
        ApprovalRequest request = request(UUID.randomUUID(), ApprovalRequestStatus.APPROVED);
        when(moderationPortOut.findForUpdate(request.getApprovalRequestId())).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () -> useCase.approve(request.getApprovalRequestId(), null));

        verify(avatarPortOut, never()).findByIdForUpdate(any(UUID.class));
    }

    private ApprovalRequest request(UUID ownerId, ApprovalRequestStatus status) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApprovalRequestId(UUID.randomUUID());
        request.setRequestType(AvatarModerationUseCaseImpl.REQUEST_TYPE);
        request.setTargetSchema(AvatarModerationUseCaseImpl.TARGET_SCHEMA);
        request.setTargetTable(AvatarModerationUseCaseImpl.TARGET_TABLE);
        request.setTargetId(UUID.randomUUID());
        request.setRequestedBy(ownerId);
        request.setStatus(status);
        request.setRequestData(Map.of());
        request.setCreatedAt(Instant.parse("2026-10-05T08:00:00Z"));
        if (status == ApprovalRequestStatus.APPROVED) {
            request.setApprovedBy(UUID.randomUUID());
            request.setApprovedAt(Instant.parse("2026-10-05T08:01:00Z"));
        }
        return request;
    }

    private UserProfileAvatar avatar(UserProfileAvatarStatus status, boolean current, String objectKey) {
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(UUID.randomUUID());
        avatar.setObjectKey(objectKey);
        avatar.setBucket(StorageBucket.PUBLIC);
        avatar.setStatus(status);
        avatar.setCurrent(current);
        return avatar;
    }
}
