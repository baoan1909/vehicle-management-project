package com.ban.vehicle_management.infrastructure.persistence.adapter.dashboard;

import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DeviceStatusItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.RevenueTrendPointResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard.VehicleTypeCountProjection;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DashboardOverviewPersistenceAdapter implements DashboardOverviewPortOut {
    private final InvoiceRepository invoiceRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final ZoneRepository zoneRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CustomerVehicleRepository customerVehicleRepository;
    private final DeviceRepository deviceRepository;

    public DashboardOverviewPersistenceAdapter(
            InvoiceRepository invoiceRepository,
            ParkingSessionRepository parkingSessionRepository,
            ZoneRepository zoneRepository,
            CardRepository cardRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            CustomerVehicleRepository customerVehicleRepository,
            DeviceRepository deviceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.zoneRepository = zoneRepository;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.customerVehicleRepository = customerVehicleRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public BigDecimal sumPaidRevenue(Instant from, Instant to) {
        BigDecimal value = invoiceRepository.sumPaidRevenue(from, to);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public long countCheckIns(Instant from, Instant to) {
        return parkingSessionRepository.countDashboardCheckIns(from, to);
    }

    @Override
    public long countCheckOuts(Instant from, Instant to) {
        return parkingSessionRepository.countDashboardCheckOuts(from, to);
    }

    @Override
    public long countParkingAt(Instant at) {
        return parkingSessionRepository.countDashboardParkingAt(at);
    }

    @Override
    public long sumActiveZoneCapacity() {
        return zoneRepository.sumDashboardActiveCapacity();
    }

    @Override
    public List<RevenueTrendPointResponse> getRevenueTrend(
            LocalDate fromDate, LocalDate toDate, Instant from, Instant to) {
        Map<LocalDate, BigDecimal> values = new LinkedHashMap<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            values.put(date, BigDecimal.ZERO);
        }
        invoiceRepository.findRevenueByDate(from, to, DateTimeUtils.getAppZone().getId())
                .forEach(row -> values.put(row.getDate(), row.getRevenue()));
        return values.entrySet().stream()
                .map(entry -> new RevenueTrendPointResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public VehicleTypeRatioResponse getVehicleTypeRatio(Instant from, Instant to) {
        var rows = parkingSessionRepository.findDashboardVehicleTypeCounts(from, to);
        long total = rows.stream().mapToLong(VehicleTypeCountProjection::getTotal).sum();
        var items = rows.stream().map(row -> new VehicleTypeRatioItemResponse(
                row.getVehicleTypeId(),
                row.getVehicleTypeName() == null ? "Không xác định" : row.getVehicleTypeName(),
                row.getTotal(),
                total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(row.getTotal())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP))).toList();
        return new VehicleTypeRatioResponse(total, items);
    }

    @Override
    public CardStatusOverviewResponse getCardStatusOverview() {
        var row = cardRepository.getDashboardStatusCounts();
        return new CardStatusOverviewResponse(row.getMemberCards(), row.getVisitorCards(), row.getLostCards());
    }

    @Override
    public long countNewAccounts(Instant from, Instant to) {
        return accountRepository.countDashboardNewAccounts(from, to);
    }

    @Override
    public long countNewCustomers(Instant from, Instant to) {
        return customerRepository.countDashboardNewCustomers(from, to);
    }

    @Override
    public long countNewCustomerVehicles(Instant from, Instant to) {
        return customerVehicleRepository.countDashboardNewVehicles(from, to);
    }

    @Override
    public List<DeviceStatusItemResponse> getDeviceStatusOverview() {
        Map<String, DeviceCounter> counters = new LinkedHashMap<>();
        counters.put("CAMERA", new DeviceCounter("CAMERA", "Camera"));
        counters.put("KIOSK", new DeviceCounter("KIOSK", "Máy tính"));
        counters.put("CARD_READER", new DeviceCounter("CARD_READER", "Đầu đọc thẻ"));
        counters.put("BARRIER", new DeviceCounter("BARRIER", "Barrier"));
        deviceRepository.findDashboardStatusCounts().forEach(row -> {
            DeviceCounter counter = counters.computeIfAbsent(row.getDeviceType(), key -> new DeviceCounter(key, key));
            switch (row.getStatus()) {
                case "ACTIVE" -> counter.activeCount += row.getTotal();
                case "OFFLINE" -> counter.offlineCount += row.getTotal();
                case "MAINTENANCE" -> counter.maintenanceCount += row.getTotal();
                default -> { }
            }
        });
        return counters.values().stream()
                .map(counter -> new DeviceStatusItemResponse(counter.deviceType, counter.deviceTypeName,
                        counter.activeCount, counter.offlineCount, counter.maintenanceCount))
                .toList();
    }

    private static class DeviceCounter {
        private final String deviceType;
        private final String deviceTypeName;
        private long activeCount;
        private long offlineCount;
        private long maintenanceCount;

        private DeviceCounter(String deviceType, String deviceTypeName) {
            this.deviceType = deviceType;
            this.deviceTypeName = deviceTypeName;
        }
    }
}
