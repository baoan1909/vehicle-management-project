package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideAdminPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.domain.notification.userguide.policy.UserGuidePolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGuideAdminUseCaseImpl implements UserGuideAdminPortIn {

    private final UserGuidePortOut guidePortOut;
    private final UserGuideFeaturePortOut featurePortOut;
    private final UserGuideVersionPortOut versionPortOut;
    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final UserGuideAuditWriter auditWriter;
    private final UserGuidePolicy policy = new UserGuidePolicy();

    public UserGuideAdminUseCaseImpl(
            UserGuidePortOut guidePortOut,
            UserGuideFeaturePortOut featurePortOut,
            UserGuideVersionPortOut versionPortOut,
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            UserGuideAuditWriter auditWriter
    ) {
        this.guidePortOut = guidePortOut;
        this.featurePortOut = featurePortOut;
        this.versionPortOut = versionPortOut;
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.auditWriter = auditWriter;
    }

    @Override
    @Transactional
    public UserGuide createGuide(UserGuide guide, UserGuideVersion initialVersion) {
        return createGuide(guide, initialVersion, null);
    }

    @Override
    @Transactional
    public UserGuide createGuide(UserGuide guide, UserGuideVersion initialVersion, String featureCode) {
        currentAccountPortIn.requirePermission("USER_GUIDE_CREATE_ALL");
        UserGuideFeature feature = resolveFeature(guide.getFeatureId(), featureCode);
        guide.setFeatureId(feature.getFeatureId());
        policy.normalizeGuide(guide);
        if (guidePortOut.existsByCode(guide.getCode())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_CODE_ALREADY_EXISTS,
                    "User guide code already exists: " + guide.getCode());
        }
        if (initialVersion == null) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_INVALID_STEP,
                    "Initial version with at least one step is required");
        }
        UserGuide savedGuide = guidePortOut.save(guide);
        initialVersion.setGuideId(savedGuide.getGuideId());
        initialVersion.setVersionNumber(1);
        validateAudienceTargets(initialVersion.getAudiences());
        policy.normalizeVersion(initialVersion);

        UserGuideVersion savedVersion = versionPortOut.save(initialVersion);
        auditWriter.record(
                "USER_GUIDE_CREATED",
                "user_guides",
                savedGuide.getGuideId(),
                Map.of("code", savedGuide.getCode(),
                        "featureCode", feature.getCode(),
                        "versionNumber", String.valueOf(savedVersion.getVersionNumber())));
        return savedGuide;
    }

    @Override
    @Transactional
    public UserGuide updateGuide(UUID guideId, UserGuide guide, Long lockVersion) {
        currentAccountPortIn.requirePermission("USER_GUIDE_UPDATE_ALL");
        UserGuide existing = loadGuide(guideId);
        requireGuideMutable(existing);
        requireLockMatch(existing, lockVersion);
        if (guide.getContextKey() != null) {
            existing.setContextKey(guide.getContextKey());
        }
        if (guide.getDisplayType() != null) {
            existing.setDisplayType(guide.getDisplayType());
        }
        if (guide.getReplayPolicy() != null) {
            existing.setReplayPolicy(guide.getReplayPolicy());
        }
        if (guide.getPriority() != null) {
            existing.setPriority(guide.getPriority());
        }
        if (guide.getDisplayOrder() != null) {
            existing.setDisplayOrder(guide.getDisplayOrder());
        }
        if (guide.getStartAt() != null) {
            existing.setStartAt(guide.getStartAt());
        }
        if (guide.getEndAt() != null || guide.getStartAt() != null) {
            existing.setEndAt(guide.getEndAt());
        }
        policy.normalizeGuide(existing);
        UserGuide saved = guidePortOut.save(existing);
        auditWriter.record(
                "USER_GUIDE_UPDATED",
                "user_guides",
                saved.getGuideId(),
                Map.of("code", saved.getCode()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public UserGuide getGuide(UUID guideId) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        return loadGuide(guideId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGuide> listGuides(
            UUID featureId,
            UserGuideStatus status,
            UserGuideDisplayType displayType,
            String keyword,
            Pageable pageable
    ) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        return guidePortOut.findPage(featureId, status, displayType, keyword, pageable);
    }

    @Override
    @Transactional
    public UserGuideVersion createDraftVersion(UUID guideId, UserGuideVersion version, Integer cloneFromVersionNumber) {
        currentAccountPortIn.requirePermission("USER_GUIDE_UPDATE_ALL");
        UserGuide guide = guidePortOut.findByIdForUpdate(guideId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND,
                        "User guide not found: " + guideId));
        requireGuideMutable(guide);
        int nextNumber = versionPortOut.findByGuideIdOrderByVersionNumberDesc(guideId).stream()
                .mapToInt(UserGuideVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;

        UserGuideVersion draft = new UserGuideVersion();
        draft.setGuideId(guideId);
        draft.setVersionNumber(nextNumber);
        if (cloneFromVersionNumber != null) {
            UserGuideVersion source = loadVersion(guideId, cloneFromVersionNumber);
            draft.setTitle(version.getTitle() != null ? version.getTitle() : source.getTitle());
            draft.setSummary(version.getSummary() != null ? version.getSummary() : source.getSummary());
            draft.setDeepLink(version.getDeepLink() != null ? version.getDeepLink() : source.getDeepLink());
            draft.setAudienceMode(version.getAudienceMode() != null ? version.getAudienceMode() : source.getAudienceMode());
            draft.setSteps(cloneSteps(
                    version.getSteps() != null && !version.getSteps().isEmpty() ? version.getSteps() : source.getSteps()));
            draft.setAudiences(cloneAudiences(
                    version.getAudiences() != null && !version.getAudiences().isEmpty()
                            ? version.getAudiences() : source.getAudiences()));
        } else {
            draft.setTitle(version.getTitle());
            draft.setSummary(version.getSummary());
            draft.setDeepLink(version.getDeepLink());
            draft.setAudienceMode(version.getAudienceMode());
            draft.setSteps(version.getSteps() == null ? List.of() : new ArrayList<>(version.getSteps()));
            draft.setAudiences(version.getAudiences() == null ? List.of() : new ArrayList<>(version.getAudiences()));
        }
        validateAudienceTargets(draft.getAudiences());
        policy.normalizeVersion(draft);
        UserGuideVersion saved = versionPortOut.save(draft);
        auditWriter.record(
                "USER_GUIDE_VERSION_CREATED",
                "user_guide_versions",
                saved.getGuideVersionId(),
                Map.of("guideId", guideId.toString(), "versionNumber", String.valueOf(saved.getVersionNumber())));
        return saved;
    }

    @Override
    @Transactional
    public UserGuideVersion updateDraftVersion(
            UUID guideId, Integer versionNumber, UserGuideVersion version, Long lockVersion) {
        currentAccountPortIn.requirePermission("USER_GUIDE_UPDATE_ALL");
        UserGuide guide = loadGuide(guideId);
        requireLockMatch(guide, lockVersion);
        UserGuideVersion existing = loadVersion(guideId, versionNumber);
        requireMutableDraft(guide, existing);
        if (version.getTitle() != null) {
            existing.setTitle(version.getTitle());
        }
        if (version.getSummary() != null) {
            existing.setSummary(version.getSummary());
        }
        if (version.getDeepLink() != null) {
            existing.setDeepLink(version.getDeepLink());
        }
        if (version.getAudienceMode() != null) {
            existing.setAudienceMode(version.getAudienceMode());
        }
        if (version.getSteps() != null) {
            existing.setSteps(new ArrayList<>(version.getSteps()));
        }
        if (version.getAudiences() != null) {
            existing.setAudiences(new ArrayList<>(version.getAudiences()));
        }
        validateAudienceTargets(existing.getAudiences());
        policy.normalizeVersion(existing);
        UserGuideVersion saved = versionPortOut.save(existing);
        auditWriter.record(
                "USER_GUIDE_VERSION_UPDATED",
                "user_guide_versions",
                saved.getGuideVersionId(),
                Map.of("guideId", guideId.toString(), "versionNumber", String.valueOf(saved.getVersionNumber())));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public UserGuideVersion getVersion(UUID guideId, Integer versionNumber) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        loadGuide(guideId);
        return loadVersion(guideId, versionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGuideVersion> listVersions(UUID guideId) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        loadGuide(guideId);
        return versionPortOut.findByGuideIdOrderByVersionNumberDesc(guideId);
    }

    @Override
    @Transactional
    public UserGuideVersion publishVersion(UUID guideId, Integer versionNumber) {
        currentAccountPortIn.requirePermission("USER_GUIDE_PUBLISH_ALL");
        UserGuide guide = guidePortOut.findByIdForUpdate(guideId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND,
                        "User guide not found: " + guideId));
        UserGuideVersion draft = loadVersion(guideId, versionNumber);
        if (draft.getVersionNumber() <= guide.getCurrentVersion()) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_PUBLISH_CONFLICT,
                    "Only a newer draft version can be published");
        }
        policy.validateTransition(guide.getStatus(), UserGuideStatus.PUBLISHED);
        UserGuideFeature feature = resolveFeature(guide.getFeatureId(), null);
        if (!Boolean.TRUE.equals(feature.getEnabled())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_PUBLISH_CONFLICT,
                    "Guide feature is disabled: " + feature.getCode());
        }
        policy.normalizeGuide(guide);
        validateAudienceTargets(draft.getAudiences());
        policy.normalizeVersion(draft);

        UUID publisherId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        draft.setPublishedAt(now);
        draft.setPublishedBy(publisherId);
        UserGuideVersion published = versionPortOut.save(draft);

        guide.setStatus(UserGuideStatus.PUBLISHED);
        guide.setCurrentVersion(published.getVersionNumber());
        guidePortOut.save(guide);

        auditWriter.record(
                "USER_GUIDE_PUBLISHED",
                "user_guides",
                guide.getGuideId(),
                Map.of("versionNumber", String.valueOf(published.getVersionNumber()),
                        "publishedBy", publisherId.toString()));
        return published;
    }

    @Override
    @Transactional
    public UserGuide archiveGuide(UUID guideId) {
        currentAccountPortIn.requirePermission("USER_GUIDE_ARCHIVE_ALL");
        UserGuide guide = loadGuide(guideId);
        policy.validateTransition(guide.getStatus(), UserGuideStatus.ARCHIVED);
        guide.setStatus(UserGuideStatus.ARCHIVED);
        UserGuide saved = guidePortOut.save(guide);
        auditWriter.record(
                "USER_GUIDE_ARCHIVED",
                "user_guides",
                saved.getGuideId(),
                Map.of("code", saved.getCode()));
        return saved;
    }

    private UserGuide loadGuide(UUID guideId) {
        return guidePortOut.findById(guideId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND,
                        "User guide not found: " + guideId));
    }

    private UserGuideVersion loadVersion(UUID guideId, Integer versionNumber) {
        if (versionNumber == null) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_VERSION_NOT_FOUND,
                    "Version number must not be null");
        }
        return versionPortOut.findByGuideIdAndVersionNumber(guideId, versionNumber)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_VERSION_NOT_FOUND,
                        "User guide version not found: guide=" + guideId + " version=" + versionNumber));
    }

    private void requireMutableDraft(UserGuide guide, UserGuideVersion version) {
        requireGuideMutable(guide);
        int current = guide.getCurrentVersion() == null ? 0 : guide.getCurrentVersion();
        if (current > 0 && version.getVersionNumber() <= current) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_VERSION_IMMUTABLE,
                    "Published version " + version.getVersionNumber() + " is immutable; create a new draft version");
        }
    }

    private void requireGuideMutable(UserGuide guide) {
        if (guide.getStatus() == UserGuideStatus.ARCHIVED) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_VERSION_IMMUTABLE,
                    "Archived user guide cannot be modified");
        }
    }

    private void requireLockMatch(UserGuide guide, Long lockVersion) {
        if (lockVersion == null || !lockVersion.equals(guide.getLockVersion())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_OPTIMISTIC_LOCK_CONFLICT,
                    "Guide was changed by another editor; reload before saving again");
        }
    }

    private UserGuideFeature resolveFeature(UUID featureId, String featureCode) {
        if (featureId != null) {
            return featurePortOut.findById(featureId)
                    .orElseThrow(() -> new UserGuideException(
                            UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND,
                            "User guide feature not found: " + featureId));
        }
        if (featureCode != null && !featureCode.isBlank()) {
            String normalized = featureCode.trim().toUpperCase(java.util.Locale.ROOT);
            return featurePortOut.findByCode(normalized)
                    .orElseThrow(() -> new UserGuideException(
                            UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND,
                            "User guide feature not found: " + normalized));
        }
        throw new UserGuideException(
                UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND,
                "featureId or featureCode must be provided");
    }

    private void validateAudienceTargets(List<UserGuideAudience> audiences) {
        if (audiences == null) {
            return;
        }
        for (UserGuideAudience audience : audiences) {
            if (audience == null || audience.getTargetType() == null) {
                continue;
            }
            switch (audience.getTargetType()) {
                case ROLE -> {
                    if (audience.getRoleId() == null
                            || rolePortOut.findById(audience.getRoleId()).isEmpty()) {
                        throw new UserGuideException(
                                UserGuideErrorCode.USER_GUIDE_INVALID_AUDIENCE,
                                "Audience role does not exist: " + audience.getRoleId());
                    }
                }
                case PERMISSION -> {
                    if (audience.getPermissionId() == null
                            || permissionPortOut.findById(audience.getPermissionId()).isEmpty()) {
                        throw new UserGuideException(
                                UserGuideErrorCode.USER_GUIDE_INVALID_AUDIENCE,
                                "Audience permission does not exist: " + audience.getPermissionId());
                    }
                }
                default -> throw new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_INVALID_AUDIENCE,
                        "Unsupported audience target type");
            }
        }
    }

    private List<UserGuideStep> cloneSteps(List<UserGuideStep> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream().map(step -> {
            UserGuideStep clone = new UserGuideStep();
            clone.setDisplayOrder(step.getDisplayOrder());
            clone.setTitle(step.getTitle());
            clone.setBody(step.getBody());
            clone.setMediaObjectKey(step.getMediaObjectKey());
            clone.setAnchorKey(step.getAnchorKey());
            clone.setPlacement(step.getPlacement());
            clone.setCollapseMs(step.getCollapseMs());
            return clone;
        }).toList();
    }

    private List<UserGuideAudience> cloneAudiences(List<UserGuideAudience> audiences) {
        if (audiences == null) {
            return List.of();
        }
        return audiences.stream().map(audience -> {
            UserGuideAudience clone = new UserGuideAudience();
            clone.setTargetType(audience.getTargetType());
            clone.setRoleId(audience.getRoleId());
            clone.setPermissionId(audience.getPermissionId());
            return clone;
        }).toList();
    }
}


