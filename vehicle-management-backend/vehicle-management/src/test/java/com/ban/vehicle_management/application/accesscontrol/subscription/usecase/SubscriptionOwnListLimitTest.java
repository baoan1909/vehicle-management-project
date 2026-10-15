package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionOwnListLimitTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut customerPortOut;

    @Mock
    private com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut ticketTypePortOut;

    @Mock
    private com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut priceRulePortOut;

    @Mock
    private com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut cardPortOut;

    @Mock
    private com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut invoicePortOut;

    @Mock
    private com.ban.vehicle_management.application.catalog.voucher.port.in.VoucherPortIn voucherPortIn;

    @Mock
    private com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut zonePortOut;

    @Mock
    private com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut parkingLotPortOut;

    @Mock
    private com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut;

    @Mock
    private com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private SubscriptionAccessGuard subscriptionAccessGuard;

    @Mock
    private com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn notificationPortIn;

    @InjectMocks
    private SubscriptionUseCaseImpl subscriptionUseCase;

    @Test
    void shouldDefaultOwnListLimitTo100() {
        when(subscriptionAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(subscriptionPortOut.findOwnSubscriptions(eq(CUSTOMER_ID), eq(100))).thenReturn(List.of());

        assertEquals(List.of(), subscriptionUseCase.getMySubscriptions(null));
    }

    @Test
    void shouldClampOwnListLimitTo200() {
        when(subscriptionAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(subscriptionPortOut.findOwnSubscriptions(eq(CUSTOMER_ID), eq(200))).thenReturn(List.of());

        subscriptionUseCase.getMySubscriptions(5000);
        verify(subscriptionPortOut).findOwnSubscriptions(eq(CUSTOMER_ID), eq(200));
    }

    @Test
    void shouldClampOwnListLimitToAtLeastOne() {
        when(subscriptionAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(subscriptionPortOut.findOwnSubscriptions(eq(CUSTOMER_ID), eq(1))).thenReturn(List.of());

        subscriptionUseCase.getMySubscriptions(0);
        verify(subscriptionPortOut).findOwnSubscriptions(eq(CUSTOMER_ID), eq(1));
    }

    @Test
    void shouldPassThroughExplicitOwnListLimit() {
        when(subscriptionAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        Subscription subscription = new Subscription();
        when(subscriptionPortOut.findOwnSubscriptions(eq(CUSTOMER_ID), eq(25))).thenReturn(List.of(subscription));

        assertEquals(List.of(subscription), subscriptionUseCase.getMySubscriptions(25));
        verify(subscriptionPortOut).findOwnSubscriptions(eq(CUSTOMER_ID), eq(25));
    }
}
