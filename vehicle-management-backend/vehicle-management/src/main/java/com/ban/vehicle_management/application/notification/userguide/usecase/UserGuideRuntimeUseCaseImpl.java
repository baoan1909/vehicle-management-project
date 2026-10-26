package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStatePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.domain.notification.userguide.policy.UserGuidePolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGuideRuntimeUseCaseImpl implements UserGuideRuntimePortIn {

    private final UserGuidePortOut guidePortOut;
    private final UserGuideFeaturePortOut featurePortOut;
    private final UserGuideVersionPortOut versionPortOut;
    private final UserGuideUserStatePortOut statePortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final PermissionPortOut permissionPortOut;
    private final FileAccessPort fileAccessPort;
    private final UserGuideAuditWriter auditWriter;
    private final UserGuidePolicy policy = new UserGuidePolicy();

    public UserGuideRuntimeUseCaseImpl(
            UserGuidePortOut guidePortOut,
            UserGuideFeaturePortOut featurePortOut,
            UserGuideVersionPortOut versionPortOut,
            UserGuideUserStatePortOut statePortOut,
            CurrentAccountPortIn currentAccountPortIn,
            PermissionPortOut permissionPortOut,
            FileAccessPort fileAccessPort,
            UserGuideAuditWriter auditWriter
    ) {
        this.guidePortOut = guidePortOut;
        this.featurePortOut = featurePortOut;
        this.versionPortOut = versionPortOut;
        this.statePortOut = statePortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.permissionPortOut = permissionPortOut;
        this.fileAccessPort = fileAccessPort;
        this.auditWriter = auditWriter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResolvedUserGuide> resolveGuides(String featureCode, String contextKey) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccount().orElse(null);
        if (featureCode == null || featureCode.isBlank()) {
            return List.of();
        }
        String normalizedFeatureCode = featureCode.trim().toUpperCase(java.util.Locale.ROOT);
        UserGuideFeature feature = featurePortOut.findByCode(normalizedFeatureCode).orElse(null);
        if (feature == null || !Boolean.TRUE.equals(feature.getEnabled())) {
            return List.of();
        }

        String normalizedContextKey = contextKey == null || contextKey.isBlank()
                ? null
                : contextKey.trim().toUpperCase(java.util.Locale.ROOT);
        Instant now = Instant.now();
        List<ResolvedUserGuide> resolved = new ArrayList<>();
        for (UserGuide guide : guidePortOut.findByFeatureIdAndStatus(feature.getFeatureId(), UserGuideStatus.PUBLISHED)) {
            if (!isScheduled(guide, now)) {
                continue;
            }
            if (normalizedContextKey != null && !normalizedContextKey.equals(guide.getContextKey())) {
                continue;
            }
            if (guide.getReplayPolicy() == UserGuideReplayPolicy.MANUAL_ONLY) {
                continue;
            }
            UserGuideVersion version = loadCurrentVersion(guide);
            if (version == null || !isEligible(account, guide, version)) {
                continue;
            }
            UserGuideUserState state = account == null
                    ? null
                    : statePortOut.findByAccountIdAndGuideId(account.accountId(), guide.getGuideId()).orElse(null);
            if (!isReplayEligible(guide.getReplayPolicy(), version.getVersionNumber(), state)) {
                continue;
            }
            resolved.add(new ResolvedUserGuide(guide, version, state, resolveMediaUrls(version)));
        }
        resolved.sort(Comparator
                .comparing((ResolvedUserGuide item) -> item.guide().getPriority())
                .thenComparing(item -> item.guide().getDisplayOrder())
                .thenComparing(item -> item.version().getPublishedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return resolved;
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedUserGuide getPublishedGuide(String guideCode) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccount().orElse(null);
        if (guideCode == null || guideCode.isBlank()) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_FOUND, "Guide code must not be blank");
        }
        UserGuide guide = guidePortOut.findByCode(guideCode.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND, "User guide not found: " + guideCode));
        if (guide.getStatus() != UserGuideStatus.PUBLISHED || !isScheduled(guide, Instant.now())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_FOUND, "User guide is not available: " + guideCode);
        }
        UserGuideVersion version = loadCurrentVersion(guide);
        if (version == null || !isEligible(account, guide, version)) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_ELIGIBLE,
                    "Current account is not eligible for guide: " + guideCode);
        }
        UserGuideUserState state = account == null
                ? null
                : statePortOut.findByAccountIdAndGuideId(account.accountId(), guide.getGuideId()).orElse(null);
        return new ResolvedUserGuide(guide, version, state, resolveMediaUrls(version));
    }

    @Override
    @Transactional
    public UserGuideUserState recordEvent(UUID guideId, UserGuideEventType event, Integer versionNumber, UUID stepId) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        if (event == null) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_INVALID_STATE_TRANSITION, "Event must not be null");
        }
        UserGuide guide = guidePortOut.findById(guideId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND, "User guide not found: " + guideId));
        if (guide.getStatus() != UserGuideStatus.PUBLISHED || !isScheduled(guide, Instant.now())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_FOUND, "User guide is not available: " + guideId);
        }
        if (guide.getCurrentVersion() == null || guide.getCurrentVersion() < 1) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_ELIGIBLE, "Guide was never published: " + guideId);
        }
        int currentVersion = guide.getCurrentVersion();
        UserGuideVersion version = versionPortOut.findByGuideIdAndVersionNumber(guideId, currentVersion)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_VERSION_NOT_FOUND,
                        "Published version not found for guide: " + guideId));
        if (!isEligible(account, guide, version)) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_NOT_ELIGIBLE,
                    "Current account is not eligible for guide: " + guide.getCode());
        }

        try {
            return applyEvent(account.accountId(), guide, version, currentVersion, event, stepId);
        } catch (DataIntegrityViolationException concurrentInsert) {
            return applyEvent(account.accountId(), guide, version, currentVersion, event, stepId);
        }
    }

    private UserGuideUserState applyEvent(
            UUID accountId,
            UserGuide guide,
            UserGuideVersion version,
            int currentVersion,
            UserGuideEventType event,
            UUID stepId
    ) {
        UserGuideUserState state = statePortOut
                .findByAccountIdAndGuideIdForUpdate(accountId, guide.getGuideId())
                .orElseGet(() -> {
                    UserGuideUserState created = new UserGuideUserState();
                    created.setAccountId(accountId);
                    created.setGuideId(guide.getGuideId());
                    return created;
                });
        Instant now = Instant.now();
        switch (event) {
            case STARTED -> {
                if (state.getStartedAt() == null) {
                    state.setStartedAt(now);
                }
                state.setLastOpenedAt(now);
            }
            case STEP_VIEWED -> {
                UserGuideStep step = requireStep(version, stepId);
                if (state.getStartedAt() == null) {
                    state.setStartedAt(now);
                }
                state.setCurrentStepId(step.getStepId());
                state.setLastOpenedAt(now);
            }
            case COMPLETED -> {
                state.setCompletedVersion(currentVersion);
                state.setCompletedAt(now);
                if (state.getDismissedVersion() != null && state.getDismissedVersion() == currentVersion) {
                    state.setDismissedVersion(null);
                    state.setDismissedAt(null);
                }
                state.setLastOpenedAt(now);
                auditWriter.record(
                        "USER_GUIDE_COMPLETED",
                        "user_guide_user_states",
                        guide.getGuideId(),
                        Map.of("versionNumber", String.valueOf(currentVersion)));
            }
            case DISMISSED -> {
                state.setDismissedVersion(currentVersion);
                state.setDismissedAt(now);
                state.setLastOpenedAt(now);
            }
            case REPLAYED -> {
                if (guide.getReplayPolicy() == UserGuideReplayPolicy.ONCE_PER_VERSION
                        && state.getCompletedVersion() != null
                        && state.getCompletedVersion() >= currentVersion) {
                    throw new UserGuideException(
                            UserGuideErrorCode.USER_GUIDE_INVALID_STATE_TRANSITION,
                            "Guide with ONCE_PER_VERSION policy cannot be replayed once completed");
                }
                state.setCompletedVersion(null);
                state.setCompletedAt(null);
                state.setDismissedVersion(null);
                state.setDismissedAt(null);
                state.setStartedAt(now);
                state.setCurrentStepId(null);
                state.setLastOpenedAt(now);
                auditWriter.record(
                        "USER_GUIDE_REPLAYED",
                        "user_guide_user_states",
                        guide.getGuideId(),
                        Map.of("versionNumber", String.valueOf(currentVersion)));
            }
            default -> throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_INVALID_STATE_TRANSITION, "Unsupported event: " + event);
        }
        policy.validateUserState(state);
        return statePortOut.save(state);
    }

    private UserGuideStep requireStep(UserGuideVersion version, UUID stepId) {
        if (stepId == null) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_INVALID_STEP, "stepId is required for STEP_VIEWED");
        }
        return version.getSteps().stream()
                .filter(step -> stepId.equals(step.getStepId()))
                .findFirst()
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_INVALID_STEP,
                        "Step does not belong to the published version: " + stepId));
    }

    private UserGuideVersion loadCurrentVersion(UserGuide guide) {
        if (guide.getCurrentVersion() == null || guide.getCurrentVersion() < 1) {
            return null;
        }
        return versionPortOut
                .findByGuideIdAndVersionNumber(guide.getGuideId(), guide.getCurrentVersion())
                .orElse(null);
    }

    private boolean isScheduled(UserGuide guide, Instant now) {
        if (guide.getStartAt() != null && now.isBefore(guide.getStartAt())) {
            return false;
        }
        return guide.getEndAt() == null || !now.isAfter(guide.getEndAt());
    }

    private boolean isEligible(CurrentAccountAccess account, UserGuide guide, UserGuideVersion version) {
        UserGuideAudienceMode mode = version.getAudienceMode();
        if (mode == UserGuideAudienceMode.PUBLIC) {
            return true;
        }
        if (mode == null || account == null) {
            return false;
        }
        if (mode == UserGuideAudienceMode.ALL_AUTHENTICATED) {
            return true;
        }
        if (mode == UserGuideAudienceMode.ROLE) {
            Set<UUID> roleTargets = version.getAudiences().stream()
                    .filter(audience -> audience.getTargetType()
                            == UserGuideAudienceTargetType.ROLE)
                    .map(UserGuideAudience::getRoleId)
                    .collect(Collectors.toSet());
            return account.roleId() != null && roleTargets.contains(account.roleId());
        }
        Set<String> permissions = account.getEffectivePermissionCodes();
        Set<String> targets = version.getAudiences().stream()
                .filter(audience -> audience.getTargetType() == UserGuideAudienceTargetType.PERMISSION)
                .map(UserGuideAudience::getPermissionId)
                .filter(id -> id != null)
                .map(permissionId -> permissionPortOut.findById(permissionId)
                        .map(permission -> permission.getPermissionCode())
                        .orElse(null))
                .filter(code -> code != null)
                .collect(Collectors.toSet());
        if (targets.isEmpty()) {
            return false;
        }
        if (mode == UserGuideAudienceMode.PERMISSION_ALL) {
            return permissions.containsAll(targets);
        }
        return targets.stream().anyMatch(permissions::contains);
    }

    private boolean isReplayEligible(
            UserGuideReplayPolicy policy, Integer currentVersion, UserGuideUserState state) {
        if (state == null || currentVersion == null) {
            return true;
        }
        if (policy == UserGuideReplayPolicy.EVERY_SESSION) {
            return true;
        }
        boolean completedCurrent = state.getCompletedVersion() != null
                && state.getCompletedVersion() >= currentVersion;
        boolean dismissedCurrent = state.getDismissedVersion() != null
                && state.getDismissedVersion() >= currentVersion;
        if (policy == UserGuideReplayPolicy.UNTIL_DISMISSED) {
            return !dismissedCurrent;
        }
        return !completedCurrent && !dismissedCurrent;
    }

    private Map<String, String> resolveMediaUrls(UserGuideVersion version) {
        Map<String, String> urls = new HashMap<>();
        for (UserGuideStep step : version.getSteps()) {
            if (step.getMediaObjectKey() == null || step.getMediaObjectKey().isBlank()) {
                continue;
            }
            try {
                urls.put(step.getMediaObjectKey(), fileAccessPort.createReadUrl(step.getMediaObjectKey()));
            } catch (RuntimeException exception) {
                urls.put(step.getMediaObjectKey(), null);
            }
        }
        return urls;
    }
}
