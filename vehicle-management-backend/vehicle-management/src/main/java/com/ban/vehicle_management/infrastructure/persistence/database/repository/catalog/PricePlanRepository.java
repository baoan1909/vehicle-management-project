package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricePlanRepository extends JpaRepository<PricePlanEntity, UUID>, JpaSpecificationExecutor<PricePlanEntity> {

    boolean existsByCode(String code);

    boolean existsByCodeAndPricePlanIdNot(String code, UUID pricePlanId);

    @Query("""
            select count(pricePlan) > 0
            from PricePlanEntity pricePlan
            where pricePlan.isActive = true
              and pricePlan.appliesTo = :appliesTo
              and (:excludedPricePlanId is null or pricePlan.pricePlanId <> :excludedPricePlanId)
              and pricePlan.effectiveFrom <= :effectiveToBoundary
              and (pricePlan.effectiveTo is null or pricePlan.effectiveTo >= :effectiveFrom)
            """)
    boolean existsActiveOverlap(
            @Param("appliesTo") PricePlanAppliesTo appliesTo,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveToBoundary") LocalDate effectiveToBoundary,
            @Param("excludedPricePlanId") UUID excludedPricePlanId
    );
}