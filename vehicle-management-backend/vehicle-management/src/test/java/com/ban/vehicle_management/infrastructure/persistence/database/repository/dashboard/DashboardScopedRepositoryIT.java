package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeAssessmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class DashboardScopedRepositoryIT extends PostgresIntegrationTestSupport {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private PlatformFeeAssessmentRepository platformFeeAssessmentRepository;
    @Autowired
    private ParkingSessionRepository parkingSessionRepository;
    @Autowired
    private ParkingLotRepository parkingLotRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @Transactional(readOnly = true)
    void scopedDashboardQueriesExecuteOnPostgres() {
        Set<UUID> parkingLotIds = Set.of(UUID.randomUUID());
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        assertEquals(0, invoiceRepository.sumPaidRevenueForParkingLots(from, to, parkingLotIds)
                .compareTo(BigDecimal.ZERO));
        assertEquals(0, platformFeeAssessmentRepository.sumAppliedFeeForParkingLots(from, to, parkingLotIds)
                .compareTo(BigDecimal.ZERO));
        assertEquals(0, parkingSessionRepository.countDashboardCheckInsForParkingLots(from, to, parkingLotIds));
        assertEquals(0, parkingSessionRepository.countDashboardCheckOutsForParkingLots(from, to, parkingLotIds));
        assertEquals(0, parkingSessionRepository.countDashboardParkingAtForParkingLots(to, parkingLotIds));
        assertNotNull(parkingSessionRepository.findDashboardVehicleTypeCountsForParkingLots(
                from, to, parkingLotIds));
        assertEquals(0, zoneRepository.sumDashboardActiveCapacityForParkingLots(parkingLotIds));
        assertNotNull(cardRepository.getDashboardStatusCountsForParkingLots(parkingLotIds));
        assertNotNull(deviceRepository.findDashboardStatusCountsForParkingLots(parkingLotIds));
        assertNotNull(parkingLotRepository.findDashboardMetrics(parkingLotIds, to));
    }
}
