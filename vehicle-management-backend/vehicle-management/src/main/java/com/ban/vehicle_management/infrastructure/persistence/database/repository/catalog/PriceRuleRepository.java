package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRuleRepository extends JpaRepository<PriceRuleEntity, UUID>, JpaSpecificationExecutor<PriceRuleEntity> {

    @Modifying
    @Query(value = """
            insert into catalog.price_rules
                (price_rule_id, organization_id, price_plan_id, vehicle_type_id, ticket_type_id,
                 rule_name, time_from, time_to, base_price, unit, lost_card_fee, priority, is_active, created_at)
            select gen_random_uuid(), :organizationId, target_plan.price_plan_id, target_vehicle.vehicle_type_id,
                   target_ticket.ticket_type_id, source.rule_name, source.time_from, source.time_to,
                   source.base_price, source.unit, source.lost_card_fee, source.priority, source.is_active, now()
            from catalog.price_rules source
            join iam.organizations template on template.organization_id = source.organization_id
            join catalog.price_plans source_plan on source_plan.price_plan_id = source.price_plan_id
            join catalog.vehicle_types source_vehicle on source_vehicle.vehicle_type_id = source.vehicle_type_id
            left join catalog.ticket_types source_ticket on source_ticket.ticket_type_id = source.ticket_type_id
            join catalog.price_plans target_plan on target_plan.organization_id = :organizationId
                and target_plan.code = source_plan.code
            join catalog.vehicle_types target_vehicle on target_vehicle.organization_id = :organizationId
                and target_vehicle.code = source_vehicle.code
            left join catalog.ticket_types target_ticket on target_ticket.organization_id = :organizationId
                and target_ticket.code = source_ticket.code
            where template.code = 'COPARKING_INTERNAL'
            """, nativeQuery = true)
    int copyInternalCatalog(@Param("organizationId") UUID organizationId);

    boolean existsByPricePlanId(UUID pricePlanId);

    boolean existsByTicketTypeIdAndIsActiveTrue(UUID ticketTypeId);

    boolean existsByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    long countByVehicleTypeIdAndIsActiveTrue(UUID vehicleTypeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PriceRuleEntity priceRule set priceRule.isActive = false where priceRule.vehicleTypeId = :vehicleTypeId and priceRule.isActive = true")
    int deactivateActiveByVehicleTypeId(@Param("vehicleTypeId") UUID vehicleTypeId);

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
        select priceRule from PriceRuleEntity priceRule
        join priceRule.pricePlan pricePlan
        join priceRule.vehicleType vehicleType
        where priceRule.isActive = true
          and priceRule.organizationId = :organizationId
          and vehicleType.canonicalVehicleTypeId = :canonicalVehicleTypeId
          and priceRule.ticketTypeId = :ticketTypeId
          and priceRule.timeFrom is null and priceRule.timeTo is null
          and pricePlan.isActive = true
          and pricePlan.appliesTo = com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo.CUSTOMER
          and pricePlan.effectiveFrom <= :effectiveDate
          and (pricePlan.effectiveTo is null or pricePlan.effectiveTo >= :effectiveDate)
        order by priceRule.priority asc, priceRule.createdAt desc
        limit 1
        """)
    Optional<PriceRuleEntity> findActiveSubscriptionRuleInOrganization(
            UUID canonicalVehicleTypeId, UUID ticketTypeId, UUID organizationId, LocalDate effectiveDate);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PriceRuleEntity item set item.isActive = false where item.organizationId in :organizationIds and item.isActive = true")
    int deactivateAllByOrganizationIds(@Param("organizationIds") Collection<UUID> organizationIds);
}
