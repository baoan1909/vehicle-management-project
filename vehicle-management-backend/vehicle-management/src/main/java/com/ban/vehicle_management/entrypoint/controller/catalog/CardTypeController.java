package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.cardtype.mapper.CardTypeApiMapper;
import com.ban.vehicle_management.application.catalog.cardtype.port.in.CardTypePortIn;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.CardTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.CreateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request.UpdateCardTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.response.CardTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/card-types")
public class CardTypeController {

    private final CardTypePortIn cardTypePortIn;
    private final CardTypeApiMapper cardTypeApiMapper;

    public CardTypeController(CardTypePortIn cardTypePortIn, CardTypeApiMapper cardTypeApiMapper) {
        this.cardTypePortIn = cardTypePortIn;
        this.cardTypeApiMapper = cardTypeApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> createCardType(@RequestBody CreateCardTypeRequest request) {
        CardType createdCardType = cardTypePortIn.createCardType(cardTypeApiMapper.toDomain(request));
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(createdCardType);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Card type created successfully", response));
    }

    @GetMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> getCardTypeById(@PathVariable UUID cardTypeId) {
        CardType cardType = cardTypePortIn.getCardTypeById(cardTypeId);
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(cardType);
        return ResponseEntity.ok(ApiResponse.ok("Fetched card type successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardTypeAdminResponse>>> getCardTypes(@ModelAttribute CardTypeFilterRequest request) {
        List<CardType> cardTypes = cardTypePortIn.getCardTypes(request.isActive());
        List<CardTypeAdminResponse> response = cardTypeApiMapper.toAdminResponses(cardTypes);
        return ResponseEntity.ok(ApiResponse.ok("Fetched card types successfully", response));
    }

    @PutMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<CardTypeAdminResponse>> updateCardType(
            @PathVariable UUID cardTypeId,
            @RequestBody UpdateCardTypeRequest request
    ) {
        CardType updatedCardType = cardTypePortIn.updateCardType(cardTypeId, cardTypeApiMapper.toDomain(request));
        CardTypeAdminResponse response = cardTypeApiMapper.toAdminResponse(updatedCardType);
        return ResponseEntity.ok(ApiResponse.ok("Card type updated successfully", response));
    }

    @DeleteMapping("/{cardTypeId}")
    public ResponseEntity<ApiResponse<Void>> deleteCardType(@PathVariable UUID cardTypeId) {
        cardTypePortIn.deleteCardType(cardTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Card type deactivated successfully"));
    }
}


