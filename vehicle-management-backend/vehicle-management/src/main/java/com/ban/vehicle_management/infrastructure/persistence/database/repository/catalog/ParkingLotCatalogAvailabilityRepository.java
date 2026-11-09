package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ParkingLotCatalogAvailabilityRepository {

    private final JdbcTemplate jdbcTemplate;

    public ParkingLotCatalogAvailabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<UUID> findExcludedVehicleTypeIds(UUID parkingLotId) {
        return jdbcTemplate.queryForList("""
                select vehicle_type_id from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = ?
                """, UUID.class, parkingLotId).stream().collect(Collectors.toUnmodifiableSet());
    }

    public Set<UUID> findExcludedTicketTypeIds(UUID parkingLotId) {
        return jdbcTemplate.queryForList("""
                select ticket_type_id from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = ?
                """, UUID.class, parkingLotId).stream().collect(Collectors.toUnmodifiableSet());
    }

    public boolean existsVehicleTypeExclusion(UUID parkingLotId, UUID vehicleTypeId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (select 1 from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = ? and vehicle_type_id = ?)
                """, Boolean.class, parkingLotId, vehicleTypeId));
    }

    public boolean existsTicketTypeExclusion(UUID parkingLotId, UUID ticketTypeId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (select 1 from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = ? and ticket_type_id = ?)
                """, Boolean.class, parkingLotId, ticketTypeId));
    }

    public void deleteVehicleTypeExclusion(UUID parkingLotId, UUID vehicleTypeId) {
        jdbcTemplate.update("""
                delete from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = ? and vehicle_type_id = ?
                """, parkingLotId, vehicleTypeId);
    }

    public void insertVehicleTypeExclusion(UUID organizationId, UUID parkingLotId, UUID vehicleTypeId) {
        jdbcTemplate.update("""
                insert into catalog.parking_lot_vehicle_type_exclusions
                    (organization_id, parking_lot_id, vehicle_type_id)
                values (?, ?, ?) on conflict do nothing
                """, organizationId, parkingLotId, vehicleTypeId);
    }

    public void deleteTicketTypeExclusion(UUID parkingLotId, UUID ticketTypeId) {
        jdbcTemplate.update("""
                delete from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = ? and ticket_type_id = ?
                """, parkingLotId, ticketTypeId);
    }

    public void insertTicketTypeExclusion(UUID organizationId, UUID parkingLotId, UUID ticketTypeId) {
        jdbcTemplate.update("""
                insert into catalog.parking_lot_ticket_type_exclusions
                    (organization_id, parking_lot_id, ticket_type_id)
                values (?, ?, ?) on conflict do nothing
                """, organizationId, parkingLotId, ticketTypeId);
    }
}
