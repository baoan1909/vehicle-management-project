package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<CardEntity, UUID>, JpaSpecificationExecutor<CardEntity> {

    boolean existsByCardTypeId(UUID cardTypeId);

    boolean existsByCardTypeIdAndStatusIn(UUID cardTypeId, Collection<CardStatus> statuses);

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<CardStatus> statuses);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUid(String uid);

    boolean existsByCardNumberAndCardIdNot(String cardNumber, UUID cardId);

    boolean existsByUidAndCardIdNot(String uid, UUID cardId);

    Optional<CardEntity> findFirstByVehicleTypeIdAndStatusOrderByCardNumberAsc(UUID vehicleTypeId, CardStatus status);
}


