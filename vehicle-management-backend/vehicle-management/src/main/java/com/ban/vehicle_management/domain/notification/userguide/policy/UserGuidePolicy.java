package com.ban.vehicle_management.domain.notification.userguide.policy;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserGuidePolicy {

    public static final int MAX_STEPS = 50;
    public static final int MAX_COLLAPSE_MS = 600_000;
    public static final int MAX_SUMMARY_LENGTH = 5_000;
    public static final int MAX_STEP_BODY_LENGTH = 10_000;
    private static final int DEFAULT_ORDER = 100;

    public void normalizeFeature(UserGuideFeature feature) {
        require(feature, "feature");
        feature.setCode(TextValidationUtils.normalizeCode(feature.getCode(), "code", 80));
        feature.setName(TextValidationUtils.normalizeRequiredText(feature.getName(), "name", 150));
        feature.setDescription(TextValidationUtils.normalizeNullableText(
                feature.getDescription(), "description", 0));
        if (feature.getEnabled() == null) {
            feature.setEnabled(true);
        }
    }

    public void normalizeGuide(UserGuide guide) {
        require(guide, "guide");
        if (guide.getFeatureId() == null) {
            throw new BadRequestException("featureId must not be null");
        }
        guide.setCode(TextValidationUtils.normalizeCode(guide.getCode(), "code", 120));
        guide.setContextKey(TextValidationUtils.normalizeCode(guide.getContextKey(), "contextKey", 120));
        if (guide.getStatus() == null) {
            guide.setStatus(UserGuideStatus.DRAFT);
        }
        if (guide.getDisplayType() == null) {
            throw new BadRequestException("displayType must not be null");
        }
        if (guide.getReplayPolicy() == null) {
            throw new BadRequestException("replayPolicy must not be null");
        }
        guide.setPriority(normalizeNonNegative(guide.getPriority(), "priority", DEFAULT_ORDER));
        guide.setDisplayOrder(normalizeNonNegative(guide.getDisplayOrder(), "displayOrder", DEFAULT_ORDER));
        if (guide.getCurrentVersion() == null) {
            guide.setCurrentVersion(0);
        } else if (guide.getCurrentVersion() < 0) {
            throw new BadRequestException("currentVersion must be greater than or equal to 0");
        }
        validateSchedule(guide.getStartAt(), guide.getEndAt());
    }

    public void normalizeVersion(UserGuideVersion version) {
        require(version, "version");
        if (version.getGuideId() == null) {
            throw new BadRequestException("guideId must not be null");
        }
        validateVersionNumber(version.getVersionNumber());
        version.setTitle(TextValidationUtils.normalizeRequiredText(version.getTitle(), "title", 255));
        version.setSummary(TextValidationUtils.normalizeNullableText(
                version.getSummary(), "summary", MAX_SUMMARY_LENGTH));
        version.setDeepLink(normalizeDeepLink(version.getDeepLink()));
        if (version.getAudienceMode() == null) {
            throw new BadRequestException("audienceMode must not be null");
        }
        normalizeSteps(version.getSteps());
        validateAudience(version.getAudienceMode(), version.getAudiences());
    }

    public void validateSchedule(Instant startAt, Instant endAt) {
        if (startAt == null) {
            throw new BadRequestException("startAt must not be null");
        }
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new BadRequestException("endAt must be after or equal to startAt");
        }
    }

    public void validateVersionNumber(Integer versionNumber) {
        if (versionNumber == null || versionNumber < 1) {
            throw new BadRequestException("versionNumber must be greater than or equal to 1");
        }
    }

    public void validateTransition(UserGuideStatus currentStatus, UserGuideStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new BadRequestException("currentStatus and targetStatus must not be null");
        }
        boolean allowed = switch (currentStatus) {
            case DRAFT -> targetStatus == UserGuideStatus.PUBLISHED
                    || targetStatus == UserGuideStatus.ARCHIVED;
            case PUBLISHED -> targetStatus == UserGuideStatus.INACTIVE
                    || targetStatus == UserGuideStatus.ARCHIVED;
            case INACTIVE -> targetStatus == UserGuideStatus.PUBLISHED
                    || targetStatus == UserGuideStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) {
            throw new ConflictException("User guide status transition is not allowed: "
                    + currentStatus + " -> " + targetStatus);
        }
    }

    public void normalizeSteps(List<UserGuideStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new BadRequestException("steps must not be empty");
        }
        if (steps.size() > MAX_STEPS) {
            throw new BadRequestException("steps must not contain more than " + MAX_STEPS + " items");
        }

        Set<Integer> orders = new HashSet<>();
        for (UserGuideStep step : steps) {
            require(step, "step");
            if (step.getDisplayOrder() == null || step.getDisplayOrder() < 0) {
                throw new BadRequestException("step.displayOrder must be greater than or equal to 0");
            }
            if (!orders.add(step.getDisplayOrder())) {
                throw new BadRequestException("step.displayOrder must be unique within a version");
            }
            step.setTitle(TextValidationUtils.normalizeRequiredText(step.getTitle(), "step.title", 255));
            step.setBody(TextValidationUtils.normalizeRequiredText(
                    step.getBody(), "step.body", MAX_STEP_BODY_LENGTH));
            step.setMediaObjectKey(normalizeMediaObjectKey(step.getMediaObjectKey()));
            step.setAnchorKey(normalizeNullableCode(step.getAnchorKey(), "step.anchorKey", 120));
            if (step.getPlacement() == null) {
                step.setPlacement(UserGuideStepPlacement.CENTER);
            }
            int collapseMs = step.getCollapseMs() == null ? 0 : step.getCollapseMs();
            if (collapseMs < 0 || collapseMs > MAX_COLLAPSE_MS) {
                throw new BadRequestException("step.collapseMs must be between 0 and " + MAX_COLLAPSE_MS);
            }
            step.setCollapseMs(collapseMs);
        }
    }

    public void validateAudience(UserGuideAudienceMode mode, List<UserGuideAudience> audiences) {
        if (mode == null) {
            throw new BadRequestException("audienceMode must not be null");
        }
        List<UserGuideAudience> safeAudiences = audiences == null ? List.of() : audiences;
        if (mode == UserGuideAudienceMode.PUBLIC || mode == UserGuideAudienceMode.ALL_AUTHENTICATED) {
            if (!safeAudiences.isEmpty()) {
                throw new BadRequestException(mode + " must not contain audience targets");
            }
            return;
        }
        if (safeAudiences.isEmpty()) {
            throw new BadRequestException(mode + " must contain at least one audience target");
        }

        UserGuideAudienceTargetType expectedType = mode == UserGuideAudienceMode.ROLE
                ? UserGuideAudienceTargetType.ROLE
                : UserGuideAudienceTargetType.PERMISSION;
        Set<UUID> targets = new HashSet<>();
        for (UserGuideAudience audience : safeAudiences) {
            require(audience, "audience");
            if (audience.getTargetType() != expectedType) {
                throw new BadRequestException("Audience target type does not match " + mode);
            }
            UUID targetId;
            if (expectedType == UserGuideAudienceTargetType.ROLE) {
                if (audience.getRoleId() == null || audience.getPermissionId() != null) {
                    throw new BadRequestException("ROLE audience requires only roleId");
                }
                targetId = audience.getRoleId();
            } else {
                if (audience.getPermissionId() == null || audience.getRoleId() != null) {
                    throw new BadRequestException("PERMISSION audience requires only permissionId");
                }
                targetId = audience.getPermissionId();
            }
            if (!targets.add(targetId)) {
                throw new BadRequestException("Audience targets must be unique within a version");
            }
        }
    }

    public void validateUserState(UserGuideUserState state) {
        require(state, "state");
        if (state.getAccountId() == null || state.getGuideId() == null) {
            throw new BadRequestException("accountId and guideId must not be null");
        }
        validateVersionTimestampPair(state.getCompletedVersion(), state.getCompletedAt(), "completed");
        validateVersionTimestampPair(state.getDismissedVersion(), state.getDismissedAt(), "dismissed");
    }

    private void validateVersionTimestampPair(Integer version, Instant timestamp, String fieldPrefix) {
        if ((version == null) != (timestamp == null)) {
            throw new BadRequestException(fieldPrefix + "Version and " + fieldPrefix
                    + "At must either both be null or both be present");
        }
        if (version != null && version < 1) {
            throw new BadRequestException(fieldPrefix + "Version must be greater than or equal to 1");
        }
    }

    private String normalizeDeepLink(String value) {
        String normalized = TextValidationUtils.normalizeNullableText(value, "deepLink", 1000);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (!normalized.startsWith("/") || normalized.startsWith("//")
                    || uri.isAbsolute() || uri.getHost() != null || normalized.contains("\\")) {
                throw new BadRequestException("deepLink must be an internal path starting with a single slash");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("deepLink must be a valid internal path");
        }
    }

    private String normalizeMediaObjectKey(String value) {
        String normalized = TextValidationUtils.normalizeNullableText(value, "step.mediaObjectKey", 500);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("/") || normalized.contains("://")
                || normalized.contains("\\") || normalized.equals("..")
                || normalized.startsWith("../") || normalized.contains("/../")) {
            throw new BadRequestException("step.mediaObjectKey must be a storage object key, not a URL or path traversal");
        }
        return normalized;
    }

    private String normalizeNullableCode(String value, String fieldName, int maxLength) {
        String normalized = TextValidationUtils.normalizeNullableText(value, fieldName, maxLength);
        return normalized == null ? null : TextValidationUtils.normalizeCode(normalized, fieldName, maxLength);
    }

    private Integer normalizeNonNegative(Integer value, String fieldName, int defaultValue) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < 0) {
            throw new BadRequestException(fieldName + " must be greater than or equal to 0");
        }
        return normalized;
    }

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}


