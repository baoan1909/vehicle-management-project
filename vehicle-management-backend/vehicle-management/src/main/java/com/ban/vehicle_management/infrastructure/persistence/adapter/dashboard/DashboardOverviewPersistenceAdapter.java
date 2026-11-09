package com.ban.vehicle_management.infrastructure.persistence.adapter.dashboard;

import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DeviceStatusItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.RevenueTrendPointResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard.DashboardOverviewQueryRepository;
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
    private final DashboardOverviewQueryRepository repository;

    public DashboardOverviewPersistenceAdapter(DashboardOverviewQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public BigDecimal sumPaidRevenue(Instant from, Instant to) {
        BigDecimal value = repository.sumPaidRevenue(from, to);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public long countCheckIns(Instant from, Instant to) {
        return repository.countCheckIns(from, to);
    }

    @Override
    public long countCheckOuts(Instant from, Instant to) {
        return repository.countCheckOuts(from, to);
    }

    @Override
    public long countParkingAt(Instant at) {
        return repository.countParkingAt(at);
    }

    @Override
    public long sumActiveZoneCapacity() {
        return repository.sumActiveZoneCapacity();
    }

    @Override
    public List<RevenueTrendPointResponse> getRevenueTrend(
            LocalDate fromDate, LocalDate toDate, Instant from, Instant to) {
        Map<LocalDate, BigDecimal> values = new LinkedHashMap<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            values.put(date, BigDecimal.ZERO);
        }
        repository.findRevenueByDate(from, to, DateTimeUtils.getAppZone().getId())
                .forEach(row -> values.put(row.date(), row.revenue()));
        return values.entrySet().stream()
                .map(entry -> new RevenueTrendPointResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public VehicleTypeRatioResponse getVehicleTypeRatio(Instant from, Instant to) {
        var rows = repository.findVehicleTypeCounts(from, to);
        long total = rows.stream().mapToLong(DashboardOverviewQueryRepository.VehicleTypeCountRow::total).sum();
        var items = rows.stream().map(row -> new VehicleTypeRatioItemResponse(
                row.vehicleTypeId(),
                row.vehicleTypeName() == null ? "Không xác định" : row.vehicleTypeName(),
                row.total(),
                total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(row.total())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP))).toList();
        return new VehicleTypeRatioResponse(total, items);
    }

    @Override
    public CardStatusOverviewResponse getCardStatusOverview() {
        var row = repository.getCardStatusCounts();
        return new CardStatusOverviewResponse(row.memberCards(), row.visitorCards(), row.lostCards());
    }

    @Override
    public long countNewAccounts(Instant from, Instant to) {
        return repository.countNewAccounts(from, to);
    }

    @Override
    public long countNewCustomers(Instant from, Instant to) {
        return repository.countNewCustomers(from, to);
    }

    @Override
    public long countNewCustomerVehicles(Instant from, Instant to) {
        return repository.countNewCustomerVehicles(from, to);
    }

    @Override
    public List<DeviceStatusItemResponse> getDeviceStatusOverview() {
        Map<String, DeviceCounter> counters = new LinkedHashMap<>();
        counters.put("CAMERA", new DeviceCounter("CAMERA", "Camera"));
        counters.put("KIOSK", new DeviceCounter("KIOSK", "Máy tính"));
        counters.put("CARD_READER", new DeviceCounter("CARD_READER", "Đầu đọc thẻ"));
        counters.put("BARRIER", new DeviceCounter("BARRIER", "Barrier"));
        repository.findDeviceStatusCounts().forEach(row -> {
            DeviceCounter counter = counters.computeIfAbsent(row.deviceType(), key -> new DeviceCounter(key, key));
            switch (row.status()) {
                case "ACTIVE" -> counter.activeCount += row.total();
                case "OFFLINE" -> counter.offlineCount += row.total();
                case "MAINTENANCE" -> counter.maintenanceCount += row.total();
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
