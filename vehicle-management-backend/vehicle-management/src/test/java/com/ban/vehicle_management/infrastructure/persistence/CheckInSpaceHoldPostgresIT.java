package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CheckInSpaceHoldPostgresIT extends PostgresIntegrationTestSupport {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    private UUID lotId;
    private UUID zoneId;
    private UUID gateId;
    private UUID laneId;
    private UUID spaceOneId;
    private UUID spaceTwoId;
    private UUID vehicleTypeId;
    private UUID operatorId;
    private UUID organizationId;

    @BeforeEach
    void setUpTopology() {
        lotId = UUID.randomUUID();
        zoneId = UUID.randomUUID();
        gateId = UUID.randomUUID();
        laneId = UUID.randomUUID();
        spaceOneId = UUID.randomUUID();
        spaceTwoId = UUID.randomUUID();
        vehicleTypeId = jdbcTemplate.queryForObject(
                "select vehicle_type_id from catalog.vehicle_types order by code limit 1", UUID.class);
        operatorId = jdbcTemplate.queryForObject(
                "select account_id from iam.accounts order by username limit 1", UUID.class);
        organizationId = jdbcTemplate.queryForObject(
                "select organization_id from iam.organizations order by created_at limit 1", UUID.class);
        jdbcTemplate.update("""
                insert into parking.parking_lots(
                    parking_lot_id, organization_id, code, name, total_capacity, status)
                values (?, ?, ?, 'Hold integration lot', 2, 'SETUP')
                """, lotId, organizationId, "HOLD-" + lotId);
        jdbcTemplate.update("""
                insert into parking.zones(zone_id, parking_lot_id, code, name, capacity, status, tracking_mode)
                values (?, ?, 'Z-HOLD', 'Hold zone', 2, 'ACTIVE', 'SPACE')
                """, zoneId, lotId);
        jdbcTemplate.update("""
                insert into parking.gates(gate_id, code, name, status, zone_id)
                values (?, 'G-HOLD', 'Hold gate', 'ACTIVE', ?)
                """, gateId, zoneId);
        jdbcTemplate.update("""
                insert into parking.lanes(lane_id, code, name, direction, status, gate_id)
                values (?, 'L-HOLD', 'Hold lane', 'IN', 'ACTIVE', ?)
                """, laneId, gateId);
        insertSpace(spaceOneId, "A-01");
        insertSpace(spaceTwoId, "A-02");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from parking.check_in_space_holds where parking_lot_id = ?", lotId);
        jdbcTemplate.update("delete from parking.lanes where lane_id = ?", laneId);
        jdbcTemplate.update("delete from parking.gates where gate_id = ?", gateId);
        jdbcTemplate.update("delete from parking.parking_spaces where zone_id = ?", zoneId);
        jdbcTemplate.update("delete from parking.zones where zone_id = ?", zoneId);
        jdbcTemplate.update("delete from parking.parking_lots where parking_lot_id = ?", lotId);
    }

    @Test
    void partialUniqueIndexReleasesSpaceOnlyAfterHoldLeavesHeldState() {
        insertHold(UUID.randomUUID(), spaceOneId, "59A11111", Instant.now().plusSeconds(120));

        assertThrows(DataIntegrityViolationException.class,
                () -> insertHold(UUID.randomUUID(), spaceOneId, "59A22222", Instant.now().plusSeconds(120)));

        jdbcTemplate.update("""
                update parking.check_in_space_holds set status = 'EXPIRED'
                where parking_space_id = ? and status = 'HELD'
                """, spaceOneId);
        insertHold(UUID.randomUUID(), spaceOneId, "59A22222", Instant.now().plusSeconds(120));
        Integer heldCount = jdbcTemplate.queryForObject("""
                select count(*) from parking.check_in_space_holds
                where parking_space_id = ? and status = 'HELD'
                """, Integer.class, spaceOneId);
        assertTrue(heldCount == 1);
    }

    @Test
    void concurrentExpiryWorkersSkipRowsLockedByAnotherWorker() throws Exception {
        insertHold(UUID.randomUUID(), spaceOneId, "59A33333", Instant.now().minusSeconds(1));
        insertHold(UUID.randomUUID(), spaceTwoId, "59A44444", Instant.now().minusSeconds(1));

        try (Connection first = dataSource.getConnection(); Connection second = dataSource.getConnection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            UUID firstId = lockNextExpired(first);
            UUID secondId = lockNextExpired(second);
            assertNotEquals(firstId, secondId);
            first.rollback();
            second.rollback();
        }
    }

    private void insertSpace(UUID spaceId, String code) {
        jdbcTemplate.update("""
                insert into parking.parking_spaces(
                    parking_space_id, zone_id, code, status, vehicle_type_id, lifecycle_status)
                values (?, ?, ?, 'AVAILABLE', ?, 'ACTIVE')
                """, spaceId, zoneId, code, vehicleTypeId);
    }

    private void insertHold(UUID holdId, UUID spaceId, String plate, Instant expiresAt) {
        jdbcTemplate.update("""
                insert into parking.check_in_space_holds(
                    hold_id, request_id, parking_space_id, parking_lot_id, zone_id, lane_id,
                    vehicle_type_id, license_plate_normalized, credential_hash, customer_type,
                    operator_account_id, status, expires_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, repeat('a', 64), 'VISITOR', ?, 'HELD', ?)
                """, holdId, UUID.randomUUID(), spaceId, lotId, zoneId, laneId,
                vehicleTypeId, plate, operatorId, Timestamp.from(expiresAt));
    }

    private UUID lockNextExpired(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                select hold_id from parking.check_in_space_holds
                where parking_lot_id = ? and status = 'HELD' and expires_at <= now()
                order by expires_at, hold_id limit 1 for update skip locked
                """)) {
            statement.setObject(1, lotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getObject(1, UUID.class);
            }
        }
    }
}
