package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<CardEntity, UUID>, JpaSpecificationExecutor<CardEntity> {

    boolean existsByCardTypeId(UUID cardTypeId);

    boolean existsByCardTypeIdAndStatusIn(UUID cardTypeId, Collection<CardStatus> statuses);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUid(String uid);

    Optional<CardEntity> findByUid(String uid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select card from CardEntity card where card.cardId = :cardId")
    Optional<CardEntity> findByIdForUpdate(@Param("cardId") UUID cardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select card
            from CardEntity card
            where card.uid = :uid
            """)
    Optional<CardEntity> findByUidForUpdate(@Param("uid") String uid);

    boolean existsByCardNumberAndCardIdNot(String cardNumber, UUID cardId);

    boolean existsByUidAndCardIdNot(String uid, UUID cardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select card
        from CardEntity card
        join card.cardType cardType
        where card.status = :status
          and upper(cardType.code) = upper(:cardTypeCode)
        order by card.cardNumber asc
        """)
    List<CardEntity> findAvailableByCardTypeCodeForUpdate(
            @Param("status") CardStatus status,
            @Param("cardTypeCode") String cardTypeCode
    );
}


