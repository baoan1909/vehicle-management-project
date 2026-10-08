package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PartnerBankAccountEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerBankAccountRepository extends JpaRepository<PartnerBankAccountEntity, UUID> {

    List<PartnerBankAccountEntity> findByOrganizationIdOrderByVerifiedDesc(UUID organizationId);
}
