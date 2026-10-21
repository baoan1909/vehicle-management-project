package com.ban.vehicle_management.application.accesscontrol.subscription.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionUserResponse;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SubscriptionApiMapperUserResponseTest {

    private final SubscriptionApiMapper mapper = Mappers.getMapper(SubscriptionApiMapper.class);

    @Test
    void shouldMapUserResponseWithoutWorkflowActors() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setParkingLotId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setRequestedEffectiveFrom(LocalDate.of(2026, 3, 1));
        subscription.setPrice(new BigDecimal("140000"));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setApprovedBy(UUID.randomUUID());
        subscription.setRejectedBy(UUID.randomUUID());
        subscription.setCreatedBy(UUID.randomUUID());
        subscription.setUpdatedBy(UUID.randomUUID());

        SubscriptionUserResponse response = mapper.toUserResponse(subscription);

        assertEquals(subscription.getSubscriptionId(), response.getSubscriptionId());
        assertEquals(subscription.getParkingLotId(), response.getParkingLotId());
        assertEquals(subscription.getCustomerVehicleId(), response.getCustomerVehicleId());
        assertEquals(new BigDecimal("140000"), response.getPrice());
        assertEquals(SubscriptionStatus.PENDING, response.getStatus());
    }

    @Test
    void shouldMapUserResponseList() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        List<SubscriptionUserResponse> responses = mapper.toUserResponses(List.of(subscription));

        assertEquals(1, responses.size());
        assertEquals(SubscriptionStatus.ACTIVE, responses.get(0).getStatus());
    }
}
