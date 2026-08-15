package com.ban.vehicle_management.entrypoint.controller.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.card.mapper.CardApiMapper;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.ChangeCardStatusPortIn;
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
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CardFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.ChangeCardStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CreateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.UpdateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response.CardAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@RequestMapping("/api/access-control/cards")
public class CardController {

    private final CardPortIn cardPortIn;
    private final ChangeCardStatusPortIn changeCardStatusPortIn;
    private final CardApiMapper cardApiMapper;
    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final CustomerPortOut customerPortOut;
    private final VehicleTypePortOut vehicleTypePortOut;
    private final TicketTypePortOut ticketTypePortOut;

    public CardController(
            CardPortIn cardPortIn,
            ChangeCardStatusPortIn changeCardStatusPortIn,
            CardApiMapper cardApiMapper,
            SubscriptionPortOut subscriptionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            CustomerPortOut customerPortOut,
            VehicleTypePortOut vehicleTypePortOut,
            TicketTypePortOut ticketTypePortOut
    ) {
        this.cardPortIn = cardPortIn;
        this.changeCardStatusPortIn = changeCardStatusPortIn;
        this.cardApiMapper = cardApiMapper;
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.customerPortOut = customerPortOut;
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.ticketTypePortOut = ticketTypePortOut;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_CREATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> createCard(@RequestBody CreateCardRequest request) {
        Card createdCard = cardPortIn.createCard(cardApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Card created successfully",
                cardApiMapper.toAdminResponse(createdCard)
        ));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_READ_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> getCardById(@PathVariable UUID cardId) {
        Card card = cardPortIn.getCardById(cardId);
        CardAdminResponse response = cardApiMapper.toAdminResponse(card);
        enrichRegisteredVehicleType(card, response);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched card successfully",
                response
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CARD_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<List<CardAdminResponse>>> getCards(@ModelAttribute CardFilterRequest request) {
        List<Card> cards = cardPortIn.getCards(
                request.status(),
                request.cardTypeId(),
                request.keyword()
        );
        List<CardAdminResponse> response = cardApiMapper.toAdminResponses(cards);
        enrichRegisteredVehicleTypes(cards, response);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched cards successfully",
                response
        ));
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> updateCard(
            @PathVariable UUID cardId,
            @RequestBody UpdateCardRequest request
    ) {
        Card updatedCard = cardPortIn.updateCard(cardId, cardApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Card updated successfully",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/status")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> changeCardStatus(
            @PathVariable UUID cardId,
            @RequestBody ChangeCardStatusRequest request
    ) {
        Card updatedCard = changeCardStatusPortIn.changeCardStatus(
                cardId,
                request.status(),
                request.blockedReason()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Card status updated successfully",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable UUID cardId) {
        cardPortIn.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok("Card retired successfully"));
    }

    private void enrichRegisteredVehicleTypes(List<Card> cards, List<CardAdminResponse> responses) {
        LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(Instant.now());
        for (int index = 0; index < cards.size(); index++) {
            Card card = cards.get(index);
            CardAdminResponse response = responses.get(index);
            enrichRegisteredVehicleType(card, response, businessDate);
        }
    }

    private void enrichRegisteredVehicleType(Card card, CardAdminResponse response) {
        enrichRegisteredVehicleType(card, response, DateTimeUtils.toVietnamLocalDate(Instant.now()));
    }

    private void enrichRegisteredVehicleType(Card card, CardAdminResponse response, LocalDate businessDate) {
        subscriptionPortOut.findActiveByCardId(card.getCardId(), businessDate)
                .or(() -> subscriptionPortOut.findLatestActiveByCardId(card.getCardId()))
                .or(() -> subscriptionPortOut.findLatestPendingCardAssignmentByCardId(card.getCardId()))
                .ifPresent(subscription -> enrichRegisteredCardContext(response, subscription));
    }

    private void enrichRegisteredCardContext(CardAdminResponse response, Subscription subscription) {
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setCustomerId(subscription.getCustomerId());
        response.setCustomerVehicleId(subscription.getCustomerVehicleId());
        response.setTicketTypeId(subscription.getTicketTypeId());
        response.setRequestedEffectiveFrom(subscription.getRequestedEffectiveFrom());
        response.setEffectiveFrom(subscription.getEffectiveFrom());
        response.setEffectiveTo(subscription.getEffectiveTo());
        response.setSubscriptionPrice(subscription.getPrice());
        response.setSubscriptionStatus(subscription.getStatus());
        response.setCardReceiptDate(subscription.getCardReceiptDate());

        if (subscription.getTicketTypeId() != null) {
            ticketTypePortOut.findById(subscription.getTicketTypeId())
                    .ifPresent(ticketType -> enrichTicketType(response, ticketType));
        }
        if (subscription.getCustomerVehicleId() != null) {
            customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                    .ifPresent(customerVehicle -> enrichCustomerVehicle(response, customerVehicle));
        }
        if (subscription.getCustomerId() != null) {
            customerPortOut.findById(subscription.getCustomerId())
                    .ifPresent(customer -> enrichCustomer(response, customer));
        }
    }

    private void enrichTicketType(CardAdminResponse response, TicketType ticketType) {
        response.setTicketTypeCode(ticketType.getCode());
        response.setTicketTypeName(ticketType.getName());
    }

    private void enrichCustomerVehicle(CardAdminResponse response, CustomerVehicle customerVehicle) {
        response.setLicensePlate(customerVehicle.getLicensePlate());
        response.setVehicleBrand(customerVehicle.getBrand());
        response.setVehicleColor(customerVehicle.getColor());
        response.setRegisteredVehicleTypeId(customerVehicle.getVehicleTypeId());

        if (customerVehicle.getVehicleTypeId() != null) {
            vehicleTypePortOut.findById(customerVehicle.getVehicleTypeId())
                    .ifPresent(vehicleType -> enrichVehicleType(response, vehicleType));
        }
    }

    private void enrichVehicleType(CardAdminResponse response, VehicleType vehicleType) {
        response.setRegisteredVehicleTypeCode(vehicleType.getCode());
        response.setRegisteredVehicleTypeName(vehicleType.getName());
    }

    private void enrichCustomer(CardAdminResponse response, Customer customer) {
        response.setCustomerCode(customer.getCustomerCode());
        response.setCustomerType(customer.getCustomerType());
        response.setCustomerStatus(customer.getStatus());
        response.setCustomerApprovalStatus(customer.getApprovalStatus());
        response.setCustomerEmail(customer.getAccountEmail());

        UserProfile userProfile = customer.getUserProfile();
        if (userProfile == null) {
            return;
        }
        response.setCustomerFullName(userProfile.getFullName());
        response.setCustomerPhoneNumber(userProfile.getPhoneNumber());
    }
}
