package com.ban.vehicle_management.application.catalog.seed.port.out;

import java.util.UUID;

public interface PartnerCatalogSeedPortOut {
    void copyInternalCatalog(UUID organizationId);
}
