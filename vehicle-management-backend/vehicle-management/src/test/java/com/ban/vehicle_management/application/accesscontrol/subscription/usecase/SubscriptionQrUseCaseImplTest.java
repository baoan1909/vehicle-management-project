package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.command.VerifySubscriptionQrCommand;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.service.SubscriptionQrTokenService;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionQrUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    private static final UUID LOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000602");

    @Mock
    private SubscriptionPortOut subscriptionPortOut;
    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;
    @Mock
    private SubscriptionAccessGuard subscriptionAccessGuard;
    @Mock
    private SubscriptionQrImagePortOut qrImagePortOut;

    private SubscriptionQrTokenService tokenService;
    private SubscriptionQrUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        tokenService = new SubscriptionQrTokenService();
        useCase = new SubscriptionQrUseCaseImpl(
                subscriptionPortOut,
                customerVehiclePortOut,
                subscriptionAccessGuard,
                tokenService,
                qrImagePortOut,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldIssueAndPersistQrForOwnActiveSubscription() {
        Subscription subscription = activeSubscription();
        when(subscriptionPortOut.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionPortOut.save(subscription)).thenReturn(subscription);
        when(qrImagePortOut.encodePngDataUrl(any())).thenReturn("data:image/png;base64,abc");

        var credential = useCase.issueMyQrCredential(SUBSCRIPTION_ID);

        verify(subscriptionAccessGuard).ensureCanReadOwn(subscription);
        verify(subscriptionPortOut).save(subscription);
        assertNotNull(subscription.getQrAccessToken());
        assertEquals(NOW, subscription.getQrIssuedAt());
        assertEquals("data:image/png;base64,abc", credential.qrCodeDataUrl());
        assertEquals(subscription.getQrAccessToken(), tokenService.extractToken(credential.qrPayload()).orElseThrow());
    }

    @Test
    void shouldRejectIssuingQrForInactiveSubscription() {
        Subscription subscription = activeSubscription();
        subscription.setStatus(SubscriptionStatus.PENDING);
        when(subscriptionPortOut.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThrows(ConflictException.class, () -> useCase.issueMyQrCredential(SUBSCRIPTION_ID));
        verify(subscriptionPortOut, never()).save(any());
    }

    @Test
    void shouldVerifyValidQrAndReturnInvalidForUnknownToken() {
        Subscription subscription = activeSubscription();
        String token = tokenService.generateToken();
        subscription.setQrAccessToken(token);
        subscription.setQrIssuedAt(NOW.minusSeconds(60));
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setLicensePlate("59A1-123.45");
        when(subscriptionPortOut.findByQrAccessToken(token)).thenReturn(Optional.of(subscription));
        when(customerVehiclePortOut.findById(subscription.getCustomerVehicleId())).thenReturn(Optional.of(vehicle));

        var valid = useCase.verifyQrCredential(new VerifySubscriptionQrCommand(
                tokenService.toPayload(token), LOT_ID, "59a112345"));
        var invalid = useCase.verifyQrCredential(new VerifySubscriptionQrCommand(
                SubscriptionQrTokenService.PAYLOAD_PREFIX + "invalid", LOT_ID, null));

        verify(subscriptionAccessGuard, times(2)).ensureCanVerifyQr();
        verify(subscriptionAccessGuard).ensureCanRead(subscription);
        assertEquals(SubscriptionQrVerificationStatus.VALID, valid.status());
        assertEquals(SubscriptionQrVerificationStatus.INVALID_QR, invalid.status());
    }

    @Test
    void shouldDecodeUploadedImageOnServerBeforeVerification() {
        Subscription subscription = activeSubscription();
        String token = tokenService.generateToken();
        String payload = tokenService.toPayload(token);
        subscription.setQrAccessToken(token);
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setLicensePlate("59A1-123.45");
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(qrImagePortOut.decode(imageBytes)).thenReturn(Optional.of(payload));
        when(subscriptionPortOut.findByQrAccessToken(token)).thenReturn(Optional.of(subscription));
        when(customerVehiclePortOut.findById(subscription.getCustomerVehicleId())).thenReturn(Optional.of(vehicle));

        var result = useCase.verifyQrImage(imageBytes, LOT_ID, "59A1-123.45");

        verify(subscriptionAccessGuard).ensureCanVerifyQr();
        assertEquals(SubscriptionQrVerificationStatus.VALID, result.status());
    }

    private Subscription activeSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        subscription.setParkingLotId(LOT_ID);
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPhysicalCardRequested(false);
        subscription.setEffectiveFrom(TODAY.minusDays(1));
        subscription.setEffectiveTo(TODAY.plusDays(30));
        return subscription;
    }
}
