package com.ban.vehicle_management.infrastructure.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.location.PostGisAdministrativeBoundaryAdapter;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIndexVersionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.adapter.ai.KnowledgeIndexVersionPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.billing.WalletTopupPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.catalog.ParkingLotCatalogAvailabilityPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.catalog.PartnerCatalogSeedPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.dashboard.DashboardOverviewPersistenceAdapter;
import com.ban.vehicle_management.infrastructure.persistence.adapter.parking.PostGisNearbyParkingLotAdapter;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletTopupOrderRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.NearbyParkingLotProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.WardRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SqlFreeAdapterDelegationTest {

    @Test
    void shouldDelegateAdministrativeBoundaryLookup() {
        var repository = mock(WardRepository.class);
        var adapter = new PostGisAdministrativeBoundaryAdapter(repository);
        BigDecimal latitude = BigDecimal.valueOf(10.7);
        BigDecimal longitude = BigDecimal.valueOf(106.6);
        when(repository.findCurrentWardCodes(latitude, longitude)).thenReturn(List.of("WARD-1"));

        assertEquals(List.of("WARD-1"), adapter.findCurrentWardCodes(latitude, longitude));
    }

    @Test
    void shouldMapTypedNearbyParkingProjection() {
        var repository = mock(ParkingLotRepository.class);
        var adapter = new PostGisNearbyParkingLotAdapter(repository);
        UUID parkingLotId = UUID.randomUUID();
        var projection = mock(NearbyParkingLotProjection.class);
        when(projection.getParkingLotId()).thenReturn(parkingLotId);
        when(projection.getName()).thenReturn("Lot");
        when(projection.getAddressDisplay()).thenReturn("Address");
        when(projection.getLatitude()).thenReturn(BigDecimal.ONE);
        when(projection.getLongitude()).thenReturn(BigDecimal.TEN);
        when(projection.getStatus()).thenReturn(ParkingLotStatus.ACTIVE);
        when(projection.getDistanceMeters()).thenReturn(12.5);
        when(repository.findNearby(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(500), 5))
                .thenReturn(List.of(projection));

        var result = adapter.findNearby(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(500), 5);

        assertEquals(parkingLotId, result.getFirst().parkingLotId());
        assertEquals(12.5, result.getFirst().distanceMeters());
    }

    @Test
    void shouldDelegateCatalogCommandsWithPersistenceSemantics() {
        UUID organizationId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        var vehicleTypeRepository = mock(VehicleTypeRepository.class);
        var ticketTypeRepository = mock(TicketTypeRepository.class);
        var pricePlanRepository = mock(PricePlanRepository.class);
        var priceRuleRepository = mock(PriceRuleRepository.class);
        new PartnerCatalogSeedPersistenceAdapter(
                vehicleTypeRepository, ticketTypeRepository, pricePlanRepository, priceRuleRepository)
                .copyInternalCatalog(organizationId);
        verify(vehicleTypeRepository).copyInternalCatalog(organizationId);
        verify(ticketTypeRepository).copyInternalCatalog(organizationId);
        verify(pricePlanRepository).copyInternalCatalog(organizationId);
        verify(priceRuleRepository).copyInternalCatalog(organizationId);

        var availabilityAdapter = new ParkingLotCatalogAvailabilityPersistenceAdapter(
                vehicleTypeRepository, ticketTypeRepository);
        availabilityAdapter.setVehicleTypeEnabled(organizationId, parkingLotId, vehicleTypeId, false);
        verify(vehicleTypeRepository).insertParkingLotExclusion(organizationId, parkingLotId, vehicleTypeId);
    }

    @Test
    void shouldDelegateDashboardCounters() {
        var parkingSessionRepository = mock(ParkingSessionRepository.class);
        var adapter = new DashboardOverviewPersistenceAdapter(
                mock(InvoiceRepository.class), parkingSessionRepository, mock(ZoneRepository.class),
                mock(CardRepository.class), mock(AccountRepository.class), mock(CustomerRepository.class),
                mock(CustomerVehicleRepository.class), mock(DeviceRepository.class));
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-11-02T00:00:00Z");
        when(parkingSessionRepository.countDashboardCheckIns(from, to)).thenReturn(7L);

        assertEquals(7L, adapter.countCheckIns(from, to));
    }

    @Test
    void shouldDelegateTransactionScopedAdvisoryLocks() {
        var walletRepository = mock(WalletTopupOrderRepository.class);
        var walletAdapter = new WalletTopupPersistenceAdapter(
                walletRepository, mock(WalletPersistenceMapper.class));
        walletAdapter.lockIdempotencyKey("key");
        verify(walletRepository).lockIdempotencyKey("key");

        var knowledgeRepository = mock(KnowledgeIndexVersionRepository.class);
        var knowledgeAdapter = new KnowledgeIndexVersionPersistenceAdapter(
                knowledgeRepository, mock(KnowledgeIndexVersionPersistenceMapper.class));
        knowledgeAdapter.lockCandidateAdvisory();
        verify(knowledgeRepository).lockCandidateAdvisory("ai_knowledge_auto_candidate");
    }
}
