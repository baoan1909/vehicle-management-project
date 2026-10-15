package com.ban.vehicle_management.application.people.customervehicle.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response.CustomerVehicleUserResponse;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerVehicleApiMapperUserResponseTest {

    private final CustomerVehicleApiMapper mapper = Mappers.getMapper(CustomerVehicleApiMapper.class);

    @Test
    void shouldMapUserResponseWithoutAuditFields() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID());
        vehicle.setCustomerId(UUID.randomUUID());
        vehicle.setVehicleTypeId(UUID.randomUUID());
        vehicle.setLicensePlate("51F-12345");
        vehicle.setBrand("Honda");
        vehicle.setColor("Red");
        vehicle.setIsDefault(true);
        vehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        vehicle.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        vehicle.setCreatedBy(UUID.randomUUID());
        vehicle.setUpdatedAt(Instant.parse("2026-02-01T00:00:00Z"));
        vehicle.setUpdatedBy(UUID.randomUUID());

        CustomerVehicleUserResponse response = mapper.toUserResponse(vehicle);

        assertEquals(vehicle.getCustomerVehicleId(), response.getCustomerVehicleId());
        assertEquals(vehicle.getVehicleTypeId(), response.getVehicleTypeId());
        assertEquals("51F-12345", response.getLicensePlate());
        assertEquals("Honda", response.getBrand());
        assertEquals(CustomerVehicleStatus.ACTIVE, response.getStatus());
        assertEquals(vehicle.getCreatedAt(), response.getCreatedAt());
        assertEquals(vehicle.getUpdatedAt(), response.getUpdatedAt());
        assertNotNull(response.getLicensePlateDisplay());
        assertNotNull(response.getLicensePlateNormalized());
    }

    @Test
    void shouldMapUserResponseList() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID());
        vehicle.setLicensePlate("51F-12345");
        vehicle.setStatus(CustomerVehicleStatus.ACTIVE);

        List<CustomerVehicleUserResponse> responses = mapper.toUserResponses(List.of(vehicle));

        assertEquals(1, responses.size());
        assertEquals(vehicle.getCustomerVehicleId(), responses.get(0).getCustomerVehicleId());
    }

    @Test
    void shouldKeepAdminMappingUnchanged() {
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(UUID.randomUUID());
        vehicle.setCustomerId(UUID.randomUUID());
        vehicle.setLicensePlate("51F-12345");
        vehicle.setStatus(CustomerVehicleStatus.ACTIVE);

        assertEquals(vehicle.getCustomerId(), mapper.toAdminResponse(vehicle).getCustomerId());
    }
}
