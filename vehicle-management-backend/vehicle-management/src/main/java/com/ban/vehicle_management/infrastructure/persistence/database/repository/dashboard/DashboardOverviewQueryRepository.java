package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardOverviewQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public DashboardOverviewQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal sumPaidRevenue(Instant from, Instant to) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(final_amount), 0) FROM billing.invoices
                WHERE status = 'PAID' AND paid_at >= ? AND paid_at < ?
                """, BigDecimal.class, from, to);
    }

    public long countCheckIns(Instant from, Instant to) {
        return queryLong("SELECT COUNT(*) FROM parking.parking_sessions WHERE check_in_time >= ? AND check_in_time < ?", from, to);
    }

    public long countCheckOuts(Instant from, Instant to) {
        return queryLong("SELECT COUNT(*) FROM parking.parking_sessions WHERE status = 'CLOSED' AND check_out_time >= ? AND check_out_time < ?", from, to);
    }

    public long countParkingAt(Instant at) {
        return queryLong("""
                SELECT COUNT(*) FROM parking.parking_sessions
                WHERE status <> 'CANCELLED' AND check_in_time < ?
                  AND (check_out_time IS NULL OR check_out_time >= ?)
                """, at, at);
    }

    public long sumActiveZoneCapacity() {
        return queryLong("SELECT COALESCE(SUM(capacity), 0) FROM parking.zones WHERE status = 'ACTIVE'");
    }

    public List<RevenueByDateRow> findRevenueByDate(Instant from, Instant to, String zoneId) {
        return jdbcTemplate.query("""
                SELECT CAST(paid_at AT TIME ZONE ? AS DATE) AS paid_date,
                       COALESCE(SUM(final_amount), 0) AS revenue
                FROM billing.invoices
                WHERE status = 'PAID' AND paid_at >= ? AND paid_at < ?
                GROUP BY paid_date ORDER BY paid_date
                """, (rs, rowNum) -> new RevenueByDateRow(
                        toLocalDate(rs.getObject("paid_date")), rs.getBigDecimal("revenue")), zoneId, from, to);
    }

    public List<VehicleTypeCountRow> findVehicleTypeCounts(Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT session.vehicle_type_id, type.name AS vehicle_type_name, COUNT(*) AS total
                FROM parking.parking_sessions session
                LEFT JOIN catalog.vehicle_types type ON type.vehicle_type_id = session.vehicle_type_id
                WHERE session.check_in_time >= ? AND session.check_in_time < ?
                GROUP BY session.vehicle_type_id, type.name ORDER BY total DESC
                """, (rs, rowNum) -> new VehicleTypeCountRow(
                        rs.getObject("vehicle_type_id", UUID.class), rs.getString("vehicle_type_name"),
                        rs.getLong("total")), from, to);
    }

    public CardStatusCountRow getCardStatusCounts() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN type.code = 'REGISTERED' AND card.status <> 'LOST' THEN 1 ELSE 0 END), 0) AS member_cards,
                       COALESCE(SUM(CASE WHEN type.code = 'VISITOR' AND card.status <> 'LOST' THEN 1 ELSE 0 END), 0) AS visitor_cards,
                       COALESCE(SUM(CASE WHEN card.status = 'LOST' THEN 1 ELSE 0 END), 0) AS lost_cards
                FROM access_control.cards card
                JOIN catalog.card_types type ON type.card_type_id = card.card_type_id
                """, (rs, rowNum) -> new CardStatusCountRow(
                        rs.getLong("member_cards"), rs.getLong("visitor_cards"), rs.getLong("lost_cards")));
    }

    public long countNewAccounts(Instant from, Instant to) {
        return queryLong("""
                SELECT COUNT(*) FROM iam.accounts WHERE created_at >= ? AND created_at < ?
                  AND status NOT IN ('PENDING_DELETION', 'DELETED')
                """, from, to);
    }

    public long countNewCustomers(Instant from, Instant to) {
        return queryLong("""
                SELECT COUNT(*) FROM people.customers customer
                JOIN iam.accounts account ON account.user_profile_id = customer.user_profile_id
                WHERE customer.created_at >= ? AND customer.created_at < ?
                  AND customer.status <> 'DELETED'
                  AND account.status NOT IN ('PENDING_DELETION', 'DELETED')
                """, from, to);
    }

    public long countNewCustomerVehicles(Instant from, Instant to) {
        return queryLong("""
                SELECT COUNT(*) FROM people.customer_vehicles vehicle
                JOIN people.customers customer ON customer.customer_id = vehicle.customer_id
                JOIN iam.accounts account ON account.user_profile_id = customer.user_profile_id
                WHERE vehicle.created_at >= ? AND vehicle.created_at < ?
                  AND customer.status <> 'DELETED'
                  AND account.status NOT IN ('PENDING_DELETION', 'DELETED')
                """, from, to);
    }

    public List<DeviceStatusCountRow> findDeviceStatusCounts() {
        return jdbcTemplate.query("""
                SELECT device_type, status, COUNT(*) AS total FROM hardware.devices
                WHERE status <> 'RETIRED' GROUP BY device_type, status
                """, (rs, rowNum) -> new DeviceStatusCountRow(
                        rs.getString("device_type"), rs.getString("status"), rs.getLong("total")));
    }

    private long queryLong(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return value == null ? 0L : value;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    public record RevenueByDateRow(LocalDate date, BigDecimal revenue) {}
    public record VehicleTypeCountRow(UUID vehicleTypeId, String vehicleTypeName, long total) {}
    public record CardStatusCountRow(long memberCards, long visitorCards, long lostCards) {}
    public record DeviceStatusCountRow(String deviceType, String status, long total) {}
}
