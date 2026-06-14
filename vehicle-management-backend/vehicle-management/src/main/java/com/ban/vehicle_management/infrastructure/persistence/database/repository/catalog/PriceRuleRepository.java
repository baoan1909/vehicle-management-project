package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import java.time.LocalTime;
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
            select count(priceRule) > 0
            from PriceRuleEntity priceRule
            where priceRule.isActive = true
              and priceRule.pricePlanId = :pricePlanId
              and priceRule.vehicleTypeId = :vehicleTypeId
              and priceRule.ticketTypeId = :ticketTypeId
              and (:excludedPriceRuleId is null or priceRule.priceRuleId <> :excludedPriceRuleId)
              and priceRule.timeFrom <= :timeTo
              and priceRule.timeTo >= :timeFrom
            """)
    boolean existsActiveVisitorTimeOverlap(
            @Param("pricePlanId") UUID pricePlanId,
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("ticketTypeId") UUID ticketTypeId,
            @Param("timeFrom") LocalTime timeFrom,
            @Param("timeTo") LocalTime timeTo,
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
}
