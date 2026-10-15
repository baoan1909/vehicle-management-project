package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.voucher.port.in.VoucherPortIn;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SubscriptionOwnCreateIdempotencyTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private CustomerPortOut customerPortOut;

    @Mock
    private TicketTypePortOut ticketTypePortOut;

    @Mock
    private PriceRulePortOut priceRulePortOut;

    @Mock
    private CardPortOut cardPortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private VoucherPortIn voucherPortIn;

    @Mock
    private ZonePortOut zonePortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private SubscriptionAccessGuard subscriptionAccessGuard;

    @Mock
    private NotificationPortIn notificationPortIn;

    @InjectMocks
    private SubscriptionUseCaseImpl subscriptionUseCase;

    @Test
    void shouldReturnExistingSubscriptionWhenIdempotencyKeyIsReused() {
        Subscription existing = new Subscription();
        existing.setSubscriptionId(UUID.randomUUID());
        existing.setCustomerId(CUSTOMER_ID);

        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);
        when(subscriptionPortOut.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, "key-1"))
                .thenReturn(Optional.of(existing));

        Subscription result = subscriptionUseCase.createOwnSubscription(new Subscription(), "key-1");

        assertEquals(existing.getSubscriptionId(), result.getSubscriptionId());
        verify(subscriptionPortOut, never()).save(any(Subscription.class));
    }

    @Test
    void shouldRejectOverlongIdempotencyKey() {
        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);

        assertThrows(BadRequestException.class, () ->
                subscriptionUseCase.createOwnSubscription(new Subscription(), "k".repeat(101)));
        verify(subscriptionPortOut, never()).save(any(Subscription.class));
    }

    @Test
    void shouldRecoverExistingSubscriptionOnUniqueConflict() {
        UUID lotId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        UUID priceRuleId = UUID.randomUUID();
        Subscription raced = new Subscription();
        raced.setSubscriptionId(UUID.randomUUID());
        raced.setCustomerId(CUSTOMER_ID);

        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(CUSTOMER_ID);
        when(subscriptionPortOut.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, "key-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(raced));

        Subscription request = new Subscription();
        request.setParkingLotId(lotId);
        request.setCustomerVehicleId(vehicleId);
        request.setTicketTypeId(ticketTypeId);
        request.setRequestedEffectiveFrom(java.time.LocalDate.now().plusDays(4));
        mockPreparation(lotId, vehicleId, vehicleTypeId, ticketTypeId, priceRuleId,
                request.getRequestedEffectiveFrom());
        when(subscriptionPortOut.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        Subscription result = subscriptionUseCase.createOwnSubscription(request, "key-race");

        assertEquals(raced.getSubscriptionId(), result.getSubscriptionId());
    }

    private void mockPreparation(UUID lotId, UUID vehicleId, UUID vehicleTypeId, UUID ticketTypeId,
            UUID priceRuleId, java.time.LocalDate effectiveFrom) {
        com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot parkingLot =
                new com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot();
        parkingLot.setParkingLotId(lotId);
        parkingLot.setOrganizationId(UUID.randomUUID());
        parkingLot.setStatus(com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus.ACTIVE);
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(parkingLot));

        com.ban.vehicle_management.domain.people.customer.model.Customer customer =
                new com.ban.vehicle_management.domain.people.customer.model.Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setStatus(com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.ACTIVE);
        customer.setApprovalStatus(com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus.APPROVED);
        when(customerPortOut.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle vehicle =
                new com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle();
        vehicle.setCustomerVehicleId(vehicleId);
        vehicle.setCustomerId(CUSTOMER_ID);
        vehicle.setVehicleTypeId(vehicleTypeId);
        vehicle.setLicensePlate("59A1-12345");
        vehicle.setStatus(com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus.ACTIVE);
        when(customerVehiclePortOut.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType ticketType =
                new com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType();
        ticketType.setTicketTypeId(ticketTypeId);
        ticketType.setOrganizationId(parkingLot.getOrganizationId());
        ticketType.setCode("MONTHLY");
        ticketType.setDurationDays(30);
        ticketType.setStatus(com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus.ACTIVE);
        when(ticketTypePortOut.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));

        when(catalogAvailabilityPortOut.isTicketTypeEnabled(lotId, ticketTypeId)).thenReturn(true);
        when(catalogAvailabilityPortOut.isVehicleTypeEnabled(lotId, vehicleTypeId)).thenReturn(true);

        com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule priceRule =
                new com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule();
        priceRule.setPriceRuleId(priceRuleId);
        priceRule.setVehicleTypeId(vehicleTypeId);
        priceRule.setTicketTypeId(ticketTypeId);
        priceRule.setBasePrice(new java.math.BigDecimal("140000"));
        when(priceRulePortOut.findActiveSubscriptionRuleInOrganization(
                vehicleTypeId, ticketTypeId, parkingLot.getOrganizationId(), effectiveFrom))
                .thenReturn(Optional.of(priceRule));
        when(subscriptionPortOut.existsOverlappingSubscriptionInParkingLot(
                any(UUID.class), any(UUID.class), any(java.time.LocalDate.class),
                any(java.time.LocalDate.class), any()))
                .thenReturn(false);
    }

    private Subscription validPendingRequest() {
        Subscription subscription = new Subscription();
        subscription.setParkingLotId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setRequestedEffectiveFrom(java.time.LocalDate.now().plusDays(1));
        return subscription;
    }
}
