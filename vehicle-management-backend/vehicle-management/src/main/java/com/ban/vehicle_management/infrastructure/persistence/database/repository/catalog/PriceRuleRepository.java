package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRuleRepository extends JpaRepository<PriceRuleEntity, UUID>, JpaSpecificationExecutor<PriceRuleEntity> {

    boolean existsByPricePlanId(UUID pricePlanId);

    boolean existsByTicketTypeIdAndIsActiveTrue(UUID ticketTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    @Query("""
        select priceRule
        from PriceRuleEntity priceRule
        where priceRule.isActive = true
          and priceRule.pricePlanId = :pricePlanId
          and priceRule.vehicleTypeId = :vehicleTypeId
          and priceRule.ticketTypeId = :ticketTypeId
          and (:excludedPriceRuleId is null or priceRule.priceRuleId <> :excludedPriceRuleId)
        """)
    List<PriceRuleEntity> findActiveVisitorRulesForOverlap(
            @Param("pricePlanId") UUID pricePlanId,
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("ticketTypeId") UUID ticketTypeId,
            @Param("excludedPriceRuleId") UUID excludedPriceRuleId
    );

    @Query("""
            select count(priceRule) > 0
            from PriceRuleEntity priceRule
            where priceRule.isActive = true
              and priceRule.pricePlanId = :pricePlanId
              and priceRule.vehicleTypeId = :vehicleTypeId
              and priceRule.ticketTypeId = :ticketTypeId
              and (:excludedPriceRuleId is null or priceRule.priceRuleId <> :excludedPriceRuleId)
            """)
    boolean existsActiveCustomerRule(
            @Param("pricePlanId") UUID pricePlanId,
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("ticketTypeId") UUID ticketTypeId,
            @Param("excludedPriceRuleId") UUID excludedPriceRuleId
    );

    @Query("""
        select priceRule
        from PriceRuleEntity priceRule
        join priceRule.pricePlan pricePlan
        where priceRule.isActive = true
          and priceRule.vehicleTypeId = :vehicleTypeId
          and priceRule.ticketTypeId = :ticketTypeId
          and priceRule.timeFrom is null
          and priceRule.timeTo is null
          and pricePlan.isActive = true
          and pricePlan.appliesTo = com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo.CUSTOMER
          and pricePlan.effectiveFrom <= :effectiveDate
          and (pricePlan.effectiveTo is null or pricePlan.effectiveTo >= :effectiveDate)
        order by priceRule.priority asc, priceRule.createdAt desc
        limit 1
        """)
    Optional<PriceRuleEntity> findActiveSubscriptionRule(
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("ticketTypeId") UUID ticketTypeId,
            @Param("effectiveDate") LocalDate effectiveDate
    );

    @Query("""
        select priceRule
        from PriceRuleEntity priceRule
        join priceRule.pricePlan pricePlan
        join priceRule.ticketType ticketType
        where priceRule.isActive = true
          and priceRule.vehicleTypeId = :vehicleTypeId
          and ticketType.code = 'DAILY'
          and priceRule.timeFrom is not null
          and priceRule.timeTo is not null
          and pricePlan.isActive = true
          and pricePlan.appliesTo = com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo.VISITOR
          and pricePlan.effectiveFrom <= :effectiveDate
          and (pricePlan.effectiveTo is null or pricePlan.effectiveTo >= :effectiveDate)
          and (
              (priceRule.timeFrom < priceRule.timeTo and priceRule.timeFrom <= :localTime and priceRule.timeTo >= :localTime)
              or
              (priceRule.timeFrom > priceRule.timeTo and (priceRule.timeFrom <= :localTime or priceRule.timeTo >= :localTime))
          )
        order by priceRule.priority asc, priceRule.createdAt desc
        limit 1
        """)
    Optional<PriceRuleEntity> findActiveVisitorRuleByTime(
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("localTime") LocalTime localTime
    );
}
