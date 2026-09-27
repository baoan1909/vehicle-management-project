package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotCatalogAvailabilityPersistenceAdapter implements ParkingLotCatalogAvailabilityPortOut {
    private final JdbcTemplate jdbcTemplate;

    public ParkingLotCatalogAvailabilityPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Set<UUID> findExcludedVehicleTypeIds(UUID parkingLotId) {
        return jdbcTemplate.queryForList("""
                select vehicle_type_id from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = ?
                """, UUID.class, parkingLotId).stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<UUID> findExcludedTicketTypeIds(UUID parkingLotId) {
        return jdbcTemplate.queryForList("""
                select ticket_type_id from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = ?
                """, UUID.class, parkingLotId).stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId) {
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (select 1 from catalog.parking_lot_vehicle_type_exclusions
                where parking_lot_id = ? and vehicle_type_id = ?)
                """, Boolean.class, parkingLotId, vehicleTypeId));
    }

    @Override
    public boolean isTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId) {
        return !Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (select 1 from catalog.parking_lot_ticket_type_exclusions
                where parking_lot_id = ? and ticket_type_id = ?)
                """, Boolean.class, parkingLotId, ticketTypeId));
    }

    @Override
    public void setVehicleTypeEnabled(UUID organizationId, UUID parkingLotId, UUID vehicleTypeId, boolean enabled) {
        if (enabled) {
            jdbcTemplate.update("""
                    delete from catalog.parking_lot_vehicle_type_exclusions
                    where parking_lot_id = ? and vehicle_type_id = ?
                    """, parkingLotId, vehicleTypeId);
        } else {
            jdbcTemplate.update("""
                    insert into catalog.parking_lot_vehicle_type_exclusions
                        (organization_id, parking_lot_id, vehicle_type_id)
                    values (?, ?, ?) on conflict do nothing
                    """, organizationId, parkingLotId, vehicleTypeId);
        }
    }

    @Override
    public void setTicketTypeEnabled(UUID organizationId, UUID parkingLotId, UUID ticketTypeId, boolean enabled) {
        if (enabled) {
            jdbcTemplate.update("""
                    delete from catalog.parking_lot_ticket_type_exclusions
                    where parking_lot_id = ? and ticket_type_id = ?
                    """, parkingLotId, ticketTypeId);
        } else {
            jdbcTemplate.update("""
                    insert into catalog.parking_lot_ticket_type_exclusions
                        (organization_id, parking_lot_id, ticket_type_id)
                    values (?, ?, ?) on conflict do nothing
                    """, organizationId, parkingLotId, ticketTypeId);
        }
    }
}
