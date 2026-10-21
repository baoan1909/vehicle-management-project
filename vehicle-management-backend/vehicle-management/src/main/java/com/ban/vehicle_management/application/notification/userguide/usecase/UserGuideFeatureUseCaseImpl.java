package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideFeaturePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.policy.UserGuidePolicy;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGuideFeatureUseCaseImpl implements UserGuideFeaturePortIn {

    private final UserGuideFeaturePortOut featurePortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final UserGuideAuditWriter auditWriter;
    private final UserGuidePolicy policy = new UserGuidePolicy();

    public UserGuideFeatureUseCaseImpl(
            UserGuideFeaturePortOut featurePortOut,
            CurrentAccountPortIn currentAccountPortIn,
            UserGuideAuditWriter auditWriter
    ) {
        this.featurePortOut = featurePortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.auditWriter = auditWriter;
    }

    @Override
    @Transactional
    public UserGuideFeature createFeature(UserGuideFeature feature) {
        currentAccountPortIn.requirePermission("USER_GUIDE_CREATE_ALL");
        policy.normalizeFeature(feature);
        if (featurePortOut.existsByCode(feature.getCode())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_CODE_ALREADY_EXISTS,
                    "User guide feature code already exists: " + feature.getCode());
        }
        UserGuideFeature saved = featurePortOut.save(feature);
        auditWriter.record(
                "USER_GUIDE_FEATURE_CREATED",
                "user_guide_features",
                saved.getFeatureId(),
                Map.of("code", saved.getCode(), "name", saved.getName()));
        return saved;
    }

    @Override
    @Transactional
    public UserGuideFeature updateFeature(UUID featureId, UserGuideFeature feature) {
        currentAccountPortIn.requirePermission("USER_GUIDE_UPDATE_ALL");
        UserGuideFeature existing = featurePortOut.findById(featureId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND,
                        "User guide feature not found: " + featureId));
        String oldName = existing.getName();
        if (feature.getName() != null) {
            existing.setName(feature.getName());
        }
        if (feature.getDescription() != null) {
            existing.setDescription(feature.getDescription());
        }
        if (feature.getEnabled() != null) {
            existing.setEnabled(feature.getEnabled());
        }
        policy.normalizeFeature(existing);
        UserGuideFeature saved = featurePortOut.save(existing);
        auditWriter.record(
                "USER_GUIDE_FEATURE_UPDATED",
                "user_guide_features",
                saved.getFeatureId(),
                Map.of("name", oldName),
                Map.of("name", saved.getName(), "enabled", String.valueOf(saved.getEnabled())));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public UserGuideFeature getFeature(UUID featureId) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        return featurePortOut.findById(featureId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_FEATURE_NOT_FOUND,
                        "User guide feature not found: " + featureId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGuideFeature> listFeatures(Boolean enabled, String keyword, Pageable pageable) {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        return featurePortOut.findPage(enabled, keyword, pageable);
    }
}
