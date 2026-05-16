package com.ban.vehicle_management.entrypoint.controller.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.card.mapper.CardApiMapper;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardUseCase;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.ChangeCardStatusUseCase;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.ChangeCardStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CreateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.UpdateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response.CardAdminResponse;
import com.ban.vehicle_management.shared.enumeration.CardStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access-control/cards")
public class CardController {

    private final CardUseCase cardUseCase;
    private final ChangeCardStatusUseCase changeCardStatusUseCase;
    private final CardApiMapper cardApiMapper;

    public CardController(
            CardUseCase cardUseCase,
            ChangeCardStatusUseCase changeCardStatusUseCase,
            CardApiMapper cardApiMapper
    ) {
        this.cardUseCase = cardUseCase;
        this.changeCardStatusUseCase = changeCardStatusUseCase;
        this.cardApiMapper = cardApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardAdminResponse>> createCard(@RequestBody CreateCardRequest request) {
        Card createdCard = cardUseCase.createCard(cardApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Card created successfully",
                cardApiMapper.toAdminResponse(createdCard)
        ));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardAdminResponse>> getCardById(@PathVariable UUID cardId) {
        Card card = cardUseCase.getCardById(cardId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched card successfully",
                cardApiMapper.toAdminResponse(card)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardAdminResponse>>> getCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) UUID cardTypeId,
            @RequestParam(required = false) UUID vehicleTypeId,
            @RequestParam(required = false) String keyword
    ) {
        List<Card> cards = cardUseCase.getCards(status, cardTypeId, vehicleTypeId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched cards successfully",
                cardApiMapper.toAdminResponses(cards)
        ));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<ApiResponse<CardAdminResponse>> updateCard(
            @PathVariable UUID cardId,
            @RequestBody UpdateCardRequest request
    ) {
        Card updatedCard = cardUseCase.updateCard(cardId, cardApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Card updated successfully",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/status")
    public ResponseEntity<ApiResponse<CardAdminResponse>> changeCardStatus(
            @PathVariable UUID cardId,
            @RequestBody ChangeCardStatusRequest request
    ) {
        Card updatedCard = changeCardStatusUseCase.changeCardStatus(
                cardId,
                request.getStatus(),
                request.getBlockedReason()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Card status updated successfully",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable UUID cardId) {
        cardUseCase.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok("Card retired successfully"));
    }
}
