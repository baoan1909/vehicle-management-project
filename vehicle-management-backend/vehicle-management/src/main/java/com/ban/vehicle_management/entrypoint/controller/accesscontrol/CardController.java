package com.ban.vehicle_management.entrypoint.controller.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.card.mapper.CardApiMapper;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardLifecyclePortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardBatchIssuancePortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardReclassificationPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardAdminQueryPortIn;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CardFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.BlockCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CreateCardBatchRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.CreateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.RecoverLostCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.ReclassifyCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.RetireCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request.UpdateCardRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response.CardAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
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
    private final CardBatchIssuancePortIn cardBatchIssuancePortIn;
    private final CardLifecyclePortIn cardLifecyclePortIn;
    private final CardReclassificationPortIn cardReclassificationPortIn;
    private final CardApiMapper cardApiMapper;
    private final CardAdminQueryPortIn cardAdminQueryPortIn;

    public CardController(
            CardPortIn cardPortIn,
            CardBatchIssuancePortIn cardBatchIssuancePortIn,
            CardLifecyclePortIn cardLifecyclePortIn,
            CardReclassificationPortIn cardReclassificationPortIn,
            CardApiMapper cardApiMapper,
            CardAdminQueryPortIn cardAdminQueryPortIn
    ) {
        this.cardPortIn = cardPortIn;
        this.cardBatchIssuancePortIn = cardBatchIssuancePortIn;
        this.cardLifecyclePortIn = cardLifecyclePortIn;
        this.cardReclassificationPortIn = cardReclassificationPortIn;
        this.cardApiMapper = cardApiMapper;
        this.cardAdminQueryPortIn = cardAdminQueryPortIn;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_CREATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> createCard(@RequestBody CreateCardRequest request) {
        Card createdCard = cardPortIn.createCard(cardApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Tạo thẻ thành công",
                cardApiMapper.toAdminResponse(createdCard)
        ));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_READ_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> getCardById(@PathVariable UUID cardId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy thông tin thẻ thành công",
                cardApiMapper.toAdminResponse(cardAdminQueryPortIn.getCardById(cardId))
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CARD_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<List<CardAdminResponse>>> getCards(@ModelAttribute CardFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách thẻ thành công",
                cardApiMapper.toAdminViewResponses(cardAdminQueryPortIn.getCards(
                        request.status(),
                        request.cardTypeId(),
                        request.parkingLotId(),
                        request.keyword()
                ))
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
                "Cập nhật thẻ thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/block")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> blockCard(
            @PathVariable UUID cardId,
            @RequestBody BlockCardRequest request
    ) {
        Card updatedCard = cardLifecyclePortIn.blockCard(cardId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(
                "Khóa thẻ thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/reclassify")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> reclassifyCard(
            @PathVariable UUID cardId,
            @RequestBody ReclassifyCardRequest request
    ) {
        Card updatedCard = cardReclassificationPortIn.reclassifyCard(cardId, request.targetCardTypeId(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok(
                "Phân loại lại thẻ thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PostMapping("/batch")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_CREATE_ALL')")
    public ResponseEntity<ApiResponse<List<CardAdminResponse>>> createCardsBatch(
            @RequestBody CreateCardBatchRequest request
    ) {
        List<Card> createdCards = cardBatchIssuancePortIn.createCards(
                request.cardTypeId(), request.quantity(), request.parkingLotId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Cấp thẻ hàng loạt thành công",
                cardApiMapper.toAdminResponses(createdCards)
        ));
    }

    @PatchMapping("/{cardId}/unblock")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> unblockCard(@PathVariable UUID cardId) {
        Card updatedCard = cardLifecyclePortIn.unblockCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Mở khóa thẻ thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/retire")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_DELETE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> retireCard(
            @PathVariable UUID cardId,
            @RequestBody RetireCardRequest request
    ) {
        Card updatedCard = cardLifecyclePortIn.retireCard(cardId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(
                "Chuyển thẻ sang trạng thái ngưng sử dụng thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @PatchMapping("/{cardId}/recover")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CardAdminResponse>> recoverLostCard(
            @PathVariable UUID cardId,
            @RequestBody RecoverLostCardRequest request
    ) {
        Card updatedCard = cardLifecyclePortIn.recoverLostCard(cardId, request.inspectionNote());
        return ResponseEntity.ok(ApiResponse.ok(
                "Thu hồi thẻ mất thành công",
                cardApiMapper.toAdminResponse(updatedCard)
        ));
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CARD_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable UUID cardId) {
        cardPortIn.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.ok("Chuyển thẻ sang trạng thái ngưng sử dụng thành công"));
    }
}
