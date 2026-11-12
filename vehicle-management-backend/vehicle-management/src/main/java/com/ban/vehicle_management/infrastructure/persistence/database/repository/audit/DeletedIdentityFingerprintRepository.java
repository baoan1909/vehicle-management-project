package com.ban.vehicle_management.infrastructure.persistence.database.repository.audit;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.DeletedIdentityFingerprintEntity;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedIdentityFingerprintRepository
        extends JpaRepository<DeletedIdentityFingerprintEntity, UUID> {
    List<DeletedIdentityFingerprintEntity> findAllByIdentityTypeAndFingerprintAndKeyVersion(
            DeletedIdentityType identityType, String fingerprint, String keyVersion);

    boolean existsByRequestIdAndIdentityTypeAndFingerprintAndKeyVersion(
            UUID requestId, DeletedIdentityType identityType, String fingerprint, String keyVersion);
}
