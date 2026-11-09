package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates independent Partner catalog rows in the organization's creation transaction. */
@Component
public class PartnerCatalogSeedPersistenceAdapter implements PartnerCatalogSeedPortOut {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PricePlanRepository pricePlanRepository;
    private final PriceRuleRepository priceRuleRepository;

    public PartnerCatalogSeedPersistenceAdapter(
            VehicleTypeRepository vehicleTypeRepository,
            TicketTypeRepository ticketTypeRepository,
            PricePlanRepository pricePlanRepository,
            PriceRuleRepository priceRuleRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.priceRuleRepository = priceRuleRepository;
    }

    @Override
    @Transactional
    public void copyInternalCatalog(UUID organizationId) {
        vehicleTypeRepository.copyInternalCatalog(organizationId);
        ticketTypeRepository.copyInternalCatalog(organizationId);
        pricePlanRepository.copyInternalCatalog(organizationId);
        priceRuleRepository.copyInternalCatalog(organizationId);
    }
}
