package com.ban.vehicle_management.infrastructure.persistence.adapter.dashboard;

import com.ban.vehicle_management.application.dashboard.overview.port.out.DashboardOverviewPortOut;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.CardStatusOverviewResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.DeviceStatusItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.RevenueTrendPointResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioItemResponse;
import com.ban.vehicle_management.entrypoint.dto.dashboard.response.VehicleTypeRatioResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DashboardOverviewPersistenceAdapter implements DashboardOverviewPortOut {

    private static final String VIETNAM_ZONE = "Asia/Ho_Chi_Minh";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BigDecimal sumPaidRevenue(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COALESCE(SUM(final_amount), 0)
                FROM billing.invoices
                WHERE status = 'PAID'
                  AND paid_at >= :fromInstant
                  AND paid_at < :toInstantExclusive
                """;

        Object result = entityManager.createNativeQuery(sql)
                .setParameter("fromInstant", fromInstant)
                .setParameter("toInstantExclusive", toInstantExclusive)
                .getSingleResult();

        return toBigDecimal(result);
    }

    @Override
    public long countCheckIns(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM parking.parking_sessions
                WHERE check_in_time >= :fromInstant
                  AND check_in_time < :toInstantExclusive
                """;

        return queryLong(sql, fromInstant, toInstantExclusive);
    }

    @Override
    public long countCheckOuts(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM parking.parking_sessions
                WHERE status = 'CLOSED'
                  AND check_out_time >= :fromInstant
                  AND check_out_time < :toInstantExclusive
                """;

        return queryLong(sql, fromInstant, toInstantExclusive);
    }

    @Override
    public long countParkingAt(Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM parking.parking_sessions
                WHERE status <> 'CANCELLED'
                  AND check_in_time < :toInstantExclusive
                  AND (
                      check_out_time IS NULL
                      OR check_out_time >= :toInstantExclusive
                  )
                """;

        Object result = entityManager.createNativeQuery(sql)
                .setParameter("toInstantExclusive", toInstantExclusive)
                .getSingleResult();

        return toLong(result);
    }

    @Override
    public long sumActiveZoneCapacity() {
        String sql = """
                SELECT COALESCE(SUM(capacity), 0)
                FROM parking.zones
                WHERE status = 'ACTIVE'
                """;

        Object result = entityManager.createNativeQuery(sql).getSingleResult();
        return toLong(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RevenueTrendPointResponse> getRevenueTrend(
            LocalDate fromDate,
            LocalDate toDate,
            Instant fromInstant,
            Instant toInstantExclusive
    ) {
        String sql = """
                SELECT CAST(paid_at AT TIME ZONE :zoneId AS DATE) AS paid_date,
                       COALESCE(SUM(final_amount), 0) AS revenue
                FROM billing.invoices
                WHERE status = 'PAID'
                  AND paid_at >= :fromInstant
                  AND paid_at < :toInstantExclusive
                GROUP BY paid_date
                ORDER BY paid_date
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("zoneId", VIETNAM_ZONE)
                .setParameter("fromInstant", fromInstant)
                .setParameter("toInstantExclusive", toInstantExclusive)
                .getResultList();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            revenueByDate.put(cursor, BigDecimal.ZERO);
            cursor = cursor.plusDays(1);
        }

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            BigDecimal revenue = toBigDecimal(row[1]);
            revenueByDate.put(date, revenue);
        }

        return revenueByDate.entrySet()
                .stream()
                .map(entry -> new RevenueTrendPointResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public VehicleTypeRatioResponse getVehicleTypeRatio(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT ps.vehicle_type_id,
                       vt.name AS vehicle_type_name,
                       COUNT(*) AS total
                FROM parking.parking_sessions ps
                LEFT JOIN catalog.vehicle_types vt
                       ON vt.vehicle_type_id = ps.vehicle_type_id
                WHERE ps.check_in_time >= :fromInstant
                  AND ps.check_in_time < :toInstantExclusive
                GROUP BY ps.vehicle_type_id, vt.name
                ORDER BY total DESC
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("fromInstant", fromInstant)
                .setParameter("toInstantExclusive", toInstantExclusive)
                .getResultList();

        long total = rows.stream()
                .mapToLong(row -> toLong(row[2]))
                .sum();

        List<VehicleTypeRatioItemResponse> items = new ArrayList<>();
        for (Object[] row : rows) {
            long count = toLong(row[2]);
            BigDecimal percentage = total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(count)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

            items.add(new VehicleTypeRatioItemResponse(
                    row[0] == null ? null : UUID.fromString(row[0].toString()),
                    row[1] == null ? "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh" : row[1].toString(),
                    count,
                    percentage
            ));
        }

        return new VehicleTypeRatioResponse(total, items);
    }

    @Override
    public CardStatusOverviewResponse getCardStatusOverview() {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN ct.code = 'REGISTERED' AND c.status <> 'LOST' THEN 1 ELSE 0 END), 0) AS member_cards,
                    COALESCE(SUM(CASE WHEN ct.code = 'VISITOR' AND c.status <> 'LOST' THEN 1 ELSE 0 END), 0) AS visitor_cards,
                    COALESCE(SUM(CASE WHEN c.status = 'LOST' THEN 1 ELSE 0 END), 0) AS lost_cards
                FROM access_control.cards c
                JOIN catalog.card_types ct
                  ON ct.card_type_id = c.card_type_id
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        return new CardStatusOverviewResponse(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2])
        );
    }

    @Override
    public long countNewAccounts(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM iam.accounts
                WHERE created_at >= :fromInstant
                  AND created_at < :toInstantExclusive
                """;

        return queryLong(sql, fromInstant, toInstantExclusive);
    }

    @Override
    public long countNewCustomers(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM people.customers
                WHERE created_at >= :fromInstant
                  AND created_at < :toInstantExclusive
                """;

        return queryLong(sql, fromInstant, toInstantExclusive);
    }

    @Override
    public long countNewCustomerVehicles(Instant fromInstant, Instant toInstantExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM people.customer_vehicles
                WHERE created_at >= :fromInstant
                  AND created_at < :toInstantExclusive
                """;

        return queryLong(sql, fromInstant, toInstantExclusive);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DeviceStatusItemResponse> getDeviceStatusOverview() {
        String sql = """
                SELECT device_type,
                       status,
                       COUNT(*) AS total
                FROM hardware.devices
                WHERE status <> 'RETIRED'
                GROUP BY device_type, status
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();

        Map<String, DeviceCounter> counters = new LinkedHashMap<>();
        counters.put("CAMERA", new DeviceCounter("CAMERA", "Camera"));
        counters.put("KIOSK", new DeviceCounter("KIOSK", "M\u00e1y t\u00ednh"));
        counters.put("CARD_READER", new DeviceCounter("CARD_READER", "\u0110\u1ea7u \u0111\u1ecdc th\u1ebb"));
        counters.put("BARRIER", new DeviceCounter("BARRIER", "Barrier"));

        for (Object[] row : rows) {
            String deviceType = row[0].toString();
            String status = row[1].toString();
            long total = toLong(row[2]);

            DeviceCounter counter = counters.computeIfAbsent(
                    deviceType,
                    key -> new DeviceCounter(key, key)
            );

            switch (status) {
                case "ACTIVE" -> counter.activeCount += total;
                case "OFFLINE" -> counter.offlineCount += total;
                case "MAINTENANCE" -> counter.maintenanceCount += total;
                default -> {
                }
            }
        }

        return counters.values()
                .stream()
                .map(counter -> new DeviceStatusItemResponse(
                        counter.deviceType,
                        counter.deviceTypeName,
                        counter.activeCount,
                        counter.offlineCount,
                        counter.maintenanceCount
                ))
                .toList();
    }

    private long queryLong(String sql, Instant fromInstant, Instant toInstantExclusive) {
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("fromInstant", fromInstant)
                .setParameter("toInstantExclusive", toInstantExclusive)
                .getSingleResult();

        return toLong(result);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
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
