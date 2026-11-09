package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.model.result.CardAdminView;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardAdminQueryPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardAdminQueryUseCaseImpl implements CardAdminQueryPortIn {

    private final CardPortIn cardPortIn;
    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final CustomerPortOut customerPortOut;
    private final VehicleTypePortOut vehicleTypePortOut;
    private final TicketTypePortOut ticketTypePortOut;
    private final Clock clock;

    public CardAdminQueryUseCaseImpl(
            CardPortIn cardPortIn,
            SubscriptionPortOut subscriptionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            CustomerPortOut customerPortOut,
            VehicleTypePortOut vehicleTypePortOut,
            TicketTypePortOut ticketTypePortOut,
            Clock clock
    ) {
        this.cardPortIn = cardPortIn;
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.customerPortOut = customerPortOut;
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.ticketTypePortOut = ticketTypePortOut;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CardAdminView getCardById(UUID cardId) {
        return enrich(cardPortIn.getCardById(cardId), businessDate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardAdminView> getCards(
            CardStatus status,
            UUID cardTypeId,
            UUID parkingLotId,
            String keyword
    ) {
        LocalDate businessDate = businessDate();
        return cardPortIn.getCards(status, cardTypeId, parkingLotId, keyword).stream()
                .map(card -> enrich(card, businessDate))
                .toList();
    }

    private CardAdminView enrich(Card card, LocalDate businessDate) {
        Subscription subscription = subscriptionPortOut.findActiveByCardId(card.getCardId(), businessDate)
                .or(() -> subscriptionPortOut.findLatestActiveByCardId(card.getCardId()))
                .or(() -> subscriptionPortOut.findLatestPendingCardAssignmentByCardId(card.getCardId()))
                .orElse(null);
        if (subscription == null) {
            return new CardAdminView(card, null, null, null, null, null);
        }

        TicketType ticketType = subscription.getTicketTypeId() == null
                ? null
                : ticketTypePortOut.findById(subscription.getTicketTypeId()).orElse(null);
        CustomerVehicle customerVehicle = subscription.getCustomerVehicleId() == null
                ? null
                : customerVehiclePortOut.findById(subscription.getCustomerVehicleId()).orElse(null);
        VehicleType registeredVehicleType = customerVehicle == null || customerVehicle.getVehicleTypeId() == null
                ? null
                : vehicleTypePortOut.findById(customerVehicle.getVehicleTypeId()).orElse(null);
        Customer customer = subscription.getCustomerId() == null
                ? null
                : customerPortOut.findById(subscription.getCustomerId()).orElse(null);
        return new CardAdminView(
                card,
                subscription,
                ticketType,
                customerVehicle,
                registeredVehicleType,
                customer
        );
    }

    private LocalDate businessDate() {
        return DateTimeUtils.toVietnamLocalDate(Instant.now(clock));
    }
}
