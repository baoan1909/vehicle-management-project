package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardLifecyclePortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardBatchIssuancePortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardReclassificationPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardNumberSeries;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardUseCaseImpl implements CardPortIn, CardLifecyclePortIn, CardBatchIssuancePortIn, CardReclassificationPortIn {

    private static final String CARD_CREATE_ALL = "CARD_CREATE_ALL";
    private static final String CARD_READ_ALL = "CARD_READ_ALL";
    private static final String CARD_UPDATE_ALL = "CARD_UPDATE_ALL";
    private static final String CARD_DELETE_ALL = "CARD_DELETE_ALL";
    private static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    private static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CardPortOut cardPort;
    private final CardTypePortOut cardTypePort;
    private final AuditLogPortOut auditLogPortOut;
    private final CardPolicy cardPolicy = new CardPolicy();

    public CardUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPort,
            CardTypePortOut cardTypePort,
            AuditLogPortOut auditLogPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.cardPort = cardPort;
        this.cardTypePort = cardTypePort;
        this.auditLogPortOut = auditLogPortOut;
    }

    @Override
    @Transactional
    public Card createCard(Card card) {
        currentAccountPortIn.requirePermission(CARD_CREATE_ALL);
        return issueCards(card.getCardTypeId(), 1).getFirst();
    }

    @Override
    @Transactional
    public List<Card> createCards(UUID cardTypeId, Integer quantity) {
        currentAccountPortIn.requirePermission(CARD_CREATE_ALL);
        if (quantity == null || quantity < 1 || quantity > 100) {
            throw new BadRequestException("Số lượng thẻ phải từ 1 đến 100");
        }

        return issueCards(cardTypeId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public Card getCardById(UUID cardId) {
        currentAccountPortIn.requirePermission(CARD_READ_ALL);
        return findExistingCard(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> getCards(CardStatus status, UUID cardTypeId, String keyword) {
        requireCardReadForOperation();
        return cardPort.findAll(status, cardTypeId, cardPolicy.normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public Card updateCard(UUID cardId, Card card) {
        currentAccountPortIn.requirePermission(CARD_UPDATE_ALL);
        throw new BadRequestException("Không hỗ trợ cập nhật trực tiếp mã thẻ, UID/RFID hoặc loại thẻ. Hãy dùng chức năng phân loại lại hoặc tái cấp/thay thẻ");
    }

    @Override
    @Transactional
    public Card reclassifyCard(UUID cardId, UUID targetCardTypeId, String reason) {
        currentAccountPortIn.requirePermission(CARD_UPDATE_ALL);
        Card existingCard = findExistingCardForUpdate(cardId);
        if (existingCard.getStatus() != CardStatus.AVAILABLE) {
            throw new BadRequestException("Chỉ có thể phân loại lại thẻ ở trạng thái sẵn sàng");
        }
        if (cardPort.hasOperationalHistory(cardId)) {
            throw new BadRequestException("Thẻ đã phát sinh nghiệp vụ không thể phân loại lại. Hãy thực hiện tái cấp hoặc thay thẻ để bảo toàn lịch sử");
        }
        if (existingCard.getCardTypeId().equals(targetCardTypeId)) {
            throw new BadRequestException("Loại thẻ mới phải khác loại thẻ hiện tại");
        }

        String normalizedReason = com.ban.vehicle_management.shared.utils.TextValidationUtils
                .normalizeRequiredText(reason, "reason", 500);
        CardType targetCardType = getRequiredCardType(targetCardTypeId);
        CardNumberSeries targetSeries = CardNumberSeries.fromCardTypeCode(targetCardType.getCode());
        String nextCardNumber = targetSeries.format(cardPort.nextCardNumberSequence(targetSeries));
        if (cardPort.existsByCardNumber(nextCardNumber)) {
            throw new ConflictException("Mã thẻ đã tồn tại");
        }

        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UUID previousCardTypeId = existingCard.getCardTypeId();
        String previousCardNumber = existingCard.getCardNumber();
        cardPolicy.reclassify(existingCard, targetCardTypeId, nextCardNumber);
        Card reclassifiedCard = cardPort.save(existingCard);
        writeReclassificationAudit(reclassifiedCard, actorAccountId, previousCardTypeId, previousCardNumber, normalizedReason);
        return reclassifiedCard;
    }

    @Override
    @Transactional
    public void deleteCard(UUID cardId) {
        currentAccountPortIn.requirePermission(CARD_DELETE_ALL);
        Card existingCard = findExistingCard(cardId);
        retireExistingCard(existingCard, "Ngừng sử dụng qua API cũ");
    }

    @Override
    @Transactional
    public Card blockCard(UUID cardId, String reason) {
        currentAccountPortIn.requirePermission(CARD_UPDATE_ALL);
        Card existingCard = findExistingCardForUpdate(cardId);
        cardPolicy.block(
                existingCard,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                reason
        );

        return cardPort.save(existingCard);
    }

    @Override
    @Transactional
    public Card unblockCard(UUID cardId) {
        currentAccountPortIn.requirePermission(CARD_UPDATE_ALL);
        Card existingCard = findExistingCardForUpdate(cardId);
        if (!cardPort.canRestoreBlockedStatus(cardId, existingCard.getStatusBeforeBlocked())) {
            throw new ConflictException("Không thể mở khóa thẻ vì trạng thái nghiệp vụ trước khi khóa không còn hợp lệ");
        }
        cardPolicy.unblock(existingCard);

        return cardPort.save(existingCard);
    }

    @Override
    @Transactional
    public Card retireCard(UUID cardId, String reason) {
        currentAccountPortIn.requirePermission(CARD_DELETE_ALL);
        Card existingCard = findExistingCardForUpdate(cardId);
        return retireExistingCard(existingCard, reason);
    }

    @Override
    @Transactional
    public Card recoverLostCard(UUID cardId, String inspectionNote) {
        currentAccountPortIn.requirePermission(CARD_UPDATE_ALL);
        Card existingCard = findExistingCardForUpdate(cardId);
        if (!cardPort.canRecoverLostCard(cardId)) {
            throw new ConflictException("Không thể thu hồi thẻ mất khi vẫn còn liên kết nghiệp vụ đang hoạt động");
        }
        cardPolicy.recover(
                existingCard,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                inspectionNote
        );

        return cardPort.save(existingCard);
    }

    private Card retireExistingCard(Card existingCard, String reason) {
        if (existingCard.getStatus() == CardStatus.RETIRED) {
            return existingCard;
        }
        if (cardPort.hasActiveUsage(existingCard.getCardId())) {
            throw new BadRequestException("Thẻ đang được sử dụng trong phiên gửi xe đang hoạt động nên không thể ngưng sử dụng. Hãy hoàn tất checkout trước");
        }

        cardPolicy.retire(
                existingCard,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                reason
        );
        return cardPort.save(existingCard);
    }

    private List<Card> issueCards(UUID cardTypeId, int quantity) {
        CardType cardType = getRequiredCardType(cardTypeId);
        CardNumberSeries cardNumberSeries = CardNumberSeries.fromCardTypeCode(cardType.getCode());
        List<Card> cards = new ArrayList<>(quantity);

        for (int index = 0; index < quantity; index++) {
            Card card = new Card();
            card.setCardTypeId(cardTypeId);
            card.setCardNumber(cardNumberSeries.format(cardPort.nextCardNumberSequence(cardNumberSeries)));
            card.setUid(UUID.randomUUID().toString());
            cardPolicy.initializeNewCard(card);

            if (cardPort.existsByCardNumber(card.getCardNumber())) {
                throw new ConflictException("Mã thẻ đã tồn tại");
            }
            if (cardPort.existsByUid(card.getUid())) {
                throw new ConflictException("UID thẻ đã tồn tại");
            }

            card.setCardId(UUID.randomUUID());
            cards.add(card);
        }

        return cardPort.saveAll(cards);
    }

    private CardType getRequiredCardType(UUID cardTypeId) {
        if (cardTypeId == null) {
            throw new BadRequestException("Loại thẻ không được để trống");
        }

        return cardTypePort.findById(cardTypeId)
                .orElseThrow(() -> new BadRequestException("Loại thẻ không tồn tại"));
    }

    private void writeReclassificationAudit(
            Card card,
            UUID actorAccountId,
            UUID previousCardTypeId,
            String previousCardNumber,
            String reason
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(actorAccountId);
        auditLog.setAction("CARD_RECLASSIFIED");
        auditLog.setTargetSchema("access_control");
        auditLog.setTargetTable("cards");
        auditLog.setTargetId(card.getCardId());
        auditLog.setOldData(Map.of(
                "cardNumber", previousCardNumber,
                "cardTypeId", previousCardTypeId,
                "uid", card.getUid()
        ));
        auditLog.setNewData(Map.of(
                "cardNumber", card.getCardNumber(),
                "cardTypeId", card.getCardTypeId(),
                "uid", card.getUid(),
                "reason", reason
        ));
        auditLogPortOut.save(auditLog);
    }

    private Card findExistingCard(UUID cardId) {
        return cardPort.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thẻ"));
    }

    private Card findExistingCardForUpdate(UUID cardId) {
        return cardPort.findByIdForUpdate(cardId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thẻ"));
    }

    private void requireCardReadForOperation() {
        if (currentAccountPortIn.hasPermission(CARD_READ_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_IN_ALL)
                || currentAccountPortIn.hasPermission(PARKING_SESSION_CHECK_OUT_ALL)) {
            return;
        }

        currentAccountPortIn.requirePermission(CARD_READ_ALL);
    }

}

