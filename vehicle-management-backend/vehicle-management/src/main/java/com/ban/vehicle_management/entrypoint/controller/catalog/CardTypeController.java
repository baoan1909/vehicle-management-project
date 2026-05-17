package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.cardtype.mapper.CardTypeApiMapper;
import com.ban.vehicle_management.application.catalog.cardtype.port.in.CardTypePortIn;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.CreateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.UpdateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.response.CardTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/card-types")
public class CardTypeController {

    private final CardTypePortIn cardTypeUseCase;
    private final CardTypeApiMapper cardTypeApiMapper;

    public CardTypeController(CardTypePortIn cardTypeUseCase, CardTypeApiMapper cardTypeApiMapper) {
        this.cardTypeUseCase = cardTypeUseCase;
        this.cardTypeApiMapper = cardTypeApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> createCardType(@RequestBody CreateCardTypeRequest request) {
        CardType createdCardType = cardTypeUseCase.createCardType(cardTypeApiMapper.toDomain(request));
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(createdCardType);
        return ResponseEntity.ok(ApiResponse.ok("Card type created successfully", response));
    }

    @GetMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> getCardTypeById(@PathVariable UUID cardTypeId) {
        CardType cardType = cardTypeUseCase.getCardTypeById(cardTypeId);
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(cardType);
        return ResponseEntity.ok(ApiResponse.ok("Fetched card type successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTypeAdminResponse>>> getCardTypes() {
        List<CardType> cardTypes = cardTypeUseCase.getCardTypes();
        List<CardTypeAdminResponse> response = cardTypeApiMapper.toAdminResponses(cardTypes);
        return ResponseEntity.ok(ApiResponse.ok("Fetched card types successfully", response));
    }

    @PutMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> updateCardType(
            @PathVariable UUID cardTypeId,
            @RequestBody UpdateCardTypeRequest request
    ) {
        CardType updatedCardType = cardTypeUseCase.updateCardType(cardTypeId, cardTypeApiMapper.toDomain(request));
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(updatedCardType);
        return ResponseEntity.ok(ApiResponse.ok("Card type updated successfully", response));
    }

    @DeleteMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<Void>> deleteCardType(@PathVariable UUID cardTypeId) {
        cardTypeUseCase.deleteCardType(cardTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Card type deactivated successfully"));
    }
}


