package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.command.VerifySubscriptionQrCommand;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrCredential;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrVerification;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionQrPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.service.SubscriptionQrTokenService;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.accesscontrol.subscription.policy.SubscriptionQrPolicy;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionQrUseCaseImpl implements SubscriptionQrPortIn {

    private static final int MAX_QR_PAYLOAD_LENGTH = 128;
    private static final int MAX_QR_IMAGE_BYTES = 5 * 1024 * 1024;

    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final SubscriptionAccessGuard subscriptionAccessGuard;
    private final SubscriptionQrTokenService tokenService;
    private final SubscriptionQrImagePortOut qrImagePortOut;
    private final Clock clock;
    private final SubscriptionQrPolicy qrPolicy = new SubscriptionQrPolicy();

    public SubscriptionQrUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            SubscriptionAccessGuard subscriptionAccessGuard,
            SubscriptionQrTokenService tokenService,
            SubscriptionQrImagePortOut qrImagePortOut,
            Clock clock
    ) {
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.subscriptionAccessGuard = subscriptionAccessGuard;
        this.tokenService = tokenService;
        this.qrImagePortOut = qrImagePortOut;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionQrCredential issueMyQrCredential(UUID subscriptionId) {
        if (subscriptionId == null) {
            throw new BadRequestException("subscriptionId must not be null");
        }
        Subscription subscription = subscriptionPortOut.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
        subscriptionAccessGuard.ensureCanReadOwn(subscription);

        boolean issued = qrPolicy.issueIfAbsent(subscription, tokenService.generateToken(), Instant.now(clock));
        Subscription persisted = issued ? subscriptionPortOut.save(subscription) : subscription;
        return toCredential(persisted);
    }

    @Override
    @Transactional
    public SubscriptionQrCredential refreshMyQrCredential(UUID subscriptionId) {
        Subscription subscription = findOwnForUpdate(subscriptionId);
        qrPolicy.refresh(subscription, tokenService.generateToken(), Instant.now(clock));
        return toCredential(subscriptionPortOut.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionQrCredential blockMySubscriptionAccess(UUID subscriptionId) {
        Subscription subscription = findForOwnAccessUpdate(subscriptionId);
        Instant now = Instant.now(clock);
        UUID actorAccountId = subscriptionAccessGuard.ensureCanUpdateOwn(subscription);
        ensureQrCredential(subscription, now);
        qrPolicy.blockAccess(subscription, actorAccountId, now);
        return toCredential(subscriptionPortOut.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionQrCredential unblockMySubscriptionAccess(UUID subscriptionId) {
        Subscription subscription = findForOwnAccessUpdate(subscriptionId);
        Instant now = Instant.now(clock);
        UUID actorAccountId = subscriptionAccessGuard.ensureCanUpdateOwn(subscription);
        ensureQrCredential(subscription, now);
        qrPolicy.unblockAccess(subscription, actorAccountId, now);
        return toCredential(subscriptionPortOut.save(subscription));
    }

    @Override
    @Transactional
    public Subscription blockSubscriptionAccess(UUID subscriptionId) {
        Subscription subscription = findForAccessUpdate(subscriptionId);
        Instant now = Instant.now(clock);
        UUID actorAccountId = subscriptionAccessGuard.ensureCanManageAccess(subscription);
        qrPolicy.blockAccess(subscription, actorAccountId, now);
        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional
    public Subscription unblockSubscriptionAccess(UUID subscriptionId) {
        Subscription subscription = findForAccessUpdate(subscriptionId);
        Instant now = Instant.now(clock);
        UUID actorAccountId = subscriptionAccessGuard.ensureCanManageAccess(subscription);
        qrPolicy.unblockAccess(subscription, actorAccountId, now);
        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionQrVerification verifyQrCredential(VerifySubscriptionQrCommand command) {
        subscriptionAccessGuard.ensureCanVerifyQr();
        return verify(command);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionQrVerification verifyQrImage(
            byte[] imageBytes,
            UUID parkingLotId,
            String licensePlate
    ) {
        subscriptionAccessGuard.ensureCanVerifyQr();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BadRequestException("QR image must not be empty");
        }
        if (imageBytes.length > MAX_QR_IMAGE_BYTES) {
            throw new BadRequestException("QR image must not exceed 5 MB");
        }
        String payload = qrImagePortOut.decode(imageBytes).orElse(null);
        if (payload == null) {
            return invalidQr();
        }
        return verify(new VerifySubscriptionQrCommand(payload, parkingLotId, licensePlate));
    }

    private SubscriptionQrVerification verify(VerifySubscriptionQrCommand command) {
        if (command == null || command.parkingLotId() == null) {
            throw new BadRequestException("parkingLotId must not be null");
        }

        String payload = TextValidationUtils.normalizeRequiredText(
                command.qrPayload(), "qrPayload", MAX_QR_PAYLOAD_LENGTH);
        var token = tokenService.extractToken(payload);
        if (token.isEmpty()) {
            return invalidQr();
        }

        Subscription subscription = subscriptionPortOut.findByQrAccessToken(token.get()).orElse(null);
        if (subscription == null) {
            return invalidQr();
        }
        subscriptionAccessGuard.ensureCanRead(subscription);

        CustomerVehicle vehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .orElseThrow(() -> new NotFoundException("Subscription vehicle not found"));
        SubscriptionQrVerificationStatus status = qrPolicy.verify(
                subscription,
                LocalDate.now(clock),
                command.parkingLotId(),
                vehicle.getLicensePlate(),
                command.licensePlate()
        );

        return new SubscriptionQrVerification(
                status,
                messageFor(status),
                payload,
                subscription.getSubscriptionId(),
                subscription.getParkingLotId(),
                subscription.getCustomerVehicleId(),
                vehicle.getLicensePlate(),
                subscription.getEffectiveFrom(),
                subscription.getEffectiveTo()
        );
    }

    private SubscriptionQrVerification invalidQr() {
        return new SubscriptionQrVerification(
                SubscriptionQrVerificationStatus.INVALID_QR,
                messageFor(SubscriptionQrVerificationStatus.INVALID_QR),
                null,
                null, null, null, null, null, null
        );
    }

    private String messageFor(SubscriptionQrVerificationStatus status) {
        return switch (status) {
            case VALID -> "Subscription QR credential is valid";
            case INVALID_QR -> "QR credential is invalid";
            case NOT_YET_VALID -> "Subscription is not effective yet";
            case EXPIRED -> "Subscription has expired";
            case REVOKED -> "QR credential has been revoked";
            case BLOCKED -> "Subscription access is temporarily blocked";
            case INACTIVE -> "Subscription is not active";
            case WRONG_PARKING_LOT -> "Subscription does not apply to this parking lot";
            case VEHICLE_MISMATCH -> "Observed license plate does not match the subscription vehicle";
        };
    }

    private Subscription findOwnForUpdate(UUID subscriptionId) {
        Subscription subscription = findForOwnAccessUpdate(subscriptionId);
        subscriptionAccessGuard.ensureCanUpdateOwn(subscription);
        return subscription;
    }

    private Subscription findForOwnAccessUpdate(UUID subscriptionId) {
        return findForAccessUpdate(subscriptionId);
    }

    private Subscription findForAccessUpdate(UUID subscriptionId) {
        if (subscriptionId == null) {
            throw new BadRequestException("subscriptionId must not be null");
        }
        return subscriptionPortOut.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
    }

    private SubscriptionQrCredential toCredential(Subscription subscription) {
        String payload = tokenService.toPayload(subscription.getQrAccessToken());
        return new SubscriptionQrCredential(
                subscription.getSubscriptionId(),
                subscription.getParkingLotId(),
                subscription.getCustomerVehicleId(),
                payload,
                qrImagePortOut.encodePngDataUrl(payload),
                subscription.getQrIssuedAt(),
                Boolean.TRUE.equals(subscription.getAccessBlocked()),
                subscription.getEffectiveFrom(),
                subscription.getEffectiveTo()
        );
    }

    private void ensureQrCredential(Subscription subscription, Instant issuedAt) {
        if (subscription.getQrAccessToken() == null || subscription.getQrRevokedAt() != null) {
            qrPolicy.refresh(subscription, tokenService.generateToken(), issuedAt);
        }
    }
}
