package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.parking.allocation.SubscriptionParkingAllocationService;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.accesscontrol.subscription.policy.SubscriptionPolicy;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.PhysicalCardFulfillmentStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountDeletionFinalizer {
    private final AccountDeletionPortOut accountDeletionPort;
    private final SubscriptionPortOut subscriptionPort;
    private final CardPortOut cardPort;
    private final SubscriptionParkingAllocationService allocationService;
    private final SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
    private final CardPolicy cardPolicy = new CardPolicy();

    public CustomerAccountDeletionFinalizer(
            AccountDeletionPortOut accountDeletionPort,
            SubscriptionPortOut subscriptionPort,
            CardPortOut cardPort,
            SubscriptionParkingAllocationService allocationService) {
        this.accountDeletionPort = accountDeletionPort;
        this.subscriptionPort = subscriptionPort;
        this.cardPort = cardPort;
        this.allocationService = allocationService;
    }

    @Transactional
    public void terminateActiveSubscriptions(AccountDeletionRequest request, Instant completedAt) {
        UUID customerId = accountDeletionPort.findDeletionCustomerId(request.accountId()).orElse(null);
        if (customerId == null) return;
        var activeSubscriptions = subscriptionPort.findAll(
                customerId, null, null, null, SubscriptionStatus.ACTIVE, null, null, null);
        if (!activeSubscriptions.isEmpty() && !request.activeSubscriptionNoRefundConfirmed()) {
            throw new IllegalStateException("ACTIVE_SUBSCRIPTION_NO_REFUND_CONSENT_REQUIRED");
        }
        for (var candidate : activeSubscriptions) {
            var subscription = subscriptionPort.findByIdForUpdate(candidate.getSubscriptionId()).orElse(null);
            if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) continue;
            allocationService.release(subscription.getSubscriptionId(), true);
            if (subscription.getCardId() != null) {
                cardPort.findByIdForUpdate(subscription.getCardId()).ifPresent(card -> {
                    cardPolicy.release(card);
                    cardPort.save(card);
                });
                subscription.setCardId(null);
                subscription.setCardReceiptDate(null);
                subscription.setPhysicalCardRequested(false);
                subscription.setPhysicalCardDeposit(BigDecimal.ZERO);
                subscription.setPhysicalCardStatus(PhysicalCardFulfillmentStatus.NOT_REQUESTED);
            }
            subscriptionPolicy.terminateForAccountDeletion(
                    subscription, request.requestId(), completedAt);
            subscriptionPort.save(subscription);
        }
    }
}
