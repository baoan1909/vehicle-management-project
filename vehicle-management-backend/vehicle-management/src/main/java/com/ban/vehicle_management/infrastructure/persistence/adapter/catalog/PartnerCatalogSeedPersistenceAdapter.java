package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PartnerCatalogSeedRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Creates independent Partner catalog rows in the organization's creation transaction. */
@Component
public class PartnerCatalogSeedPersistenceAdapter implements PartnerCatalogSeedPortOut {

    private final PartnerCatalogSeedRepository repository;

    public PartnerCatalogSeedPersistenceAdapter(PartnerCatalogSeedRepository repository) {
        this.repository = repository;
    }

    @Override
    public void copyInternalCatalog(UUID organizationId) {
        repository.copyInternalCatalog(organizationId);
    }
}
