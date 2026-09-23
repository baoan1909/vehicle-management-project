package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiMessageCitationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageCitationRepository extends JpaRepository<AiMessageCitationEntity, UUID> {

    List<AiMessageCitationEntity> findByMessageIdOrderByCitationOrderAsc(UUID messageId);
}
