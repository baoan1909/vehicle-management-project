package com.ban.vehicle_management.application.accesscontrol.card.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardPortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.in.CardLifecyclePortIn;
import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardUseCaseImpl implements CardPortIn, CardLifecyclePortIn {

    private static final String CARD_CREATE_ALL = "CARD_CREATE_ALL";
    private static final String CARD_READ_ALL = "CARD_READ_ALL";
    private static final String CARD_UPDATE_ALL = "CARD_UPDATE_ALL";
    private static final String CARD_DELETE_ALL = "CARD_DELETE_ALL";
    private static final String PARKING_SESSION_CHECK_IN_ALL = "PARKING_SESSION_CHECK_IN_ALL";
    private static final String PARKING_SESSION_CHECK_OUT_ALL = "PARKING_SESSION_CHECK_OUT_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CardPortOut cardPort;
    private final CardTypePortOut cardTypePort;
    private final CardPolicy cardPolicy = new CardPolicy();

    public CardUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            CardPortOut cardPort,
            CardTypePortOut cardTypePort
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.cardPort = cardPort;
        this.cardTypePort = cardTypePort;
    }

    @Override
    @Transactional
    public Card createCard(Card card) {
        currentAccountPortIn.requirePermission(CARD_CREATE_ALL);
        cardPolicy.initializeNewCard(card);
        validateCardTypeExists(card.getCardTypeId());

        if (cardPort.existsByCardNumber(card.getCardNumber())) {
            throw new ConflictException("Mã thẻ đã tồn tại");
        }
        if (cardPort.existsByUid(card.getUid())) {
            throw new ConflictException("UID thẻ đã tồn tại");
        }

        card.setCardId(UUID.randomUUID());
        return cardPort.save(card);
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
        Card existingCard = findExistingCard(cardId);

        if (existingCard.getStatus() == CardStatus.IN_USE) {
            throw new BadRequestException("Không thể cập nhật thẻ đang được sử dụng");
        }

        if (existingCard.getStatus() != CardStatus.AVAILABLE
                && !Objects.equals(existingCard.getCardTypeId(), card.getCardTypeId())) {
            throw new BadRequestException("Chỉ có thể đổi loại thẻ khi thẻ ở trạng thái sẵn sàng");
        }

        if (cardPolicy.hasCoreIdentifierChanged(existingCard, card) && cardPort.hasOperationalHistory(cardId)) {
            throw new BadRequestException("Không thể thay đổi mã thẻ hoặc UID sau khi thẻ đã được sử dụng trong nghiệp vụ");
        }

        existingCard.setCardNumber(card.getCardNumber());
        existingCard.setUid(card.getUid());
        existingCard.setCardTypeId(card.getCardTypeId());
        cardPolicy.validateMaintenance(existingCard);
        validateCardTypeExists(existingCard.getCardTypeId());

        if (cardPort.existsByCardNumberAndCardIdNot(existingCard.getCardNumber(), cardId)) {
            throw new ConflictException("Mã thẻ đã tồn tại");
        }
        if (cardPort.existsByUidAndCardIdNot(existingCard.getUid(), cardId)) {
            throw new ConflictException("UID thẻ đã tồn tại");
        }

        return cardPort.save(existingCard);
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

    private void validateCardTypeExists(UUID cardTypeId) {
        if (cardTypePort.findById(cardTypeId).isEmpty()) {
            throw new BadRequestException("Loại thẻ không tồn tại");
        }
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

