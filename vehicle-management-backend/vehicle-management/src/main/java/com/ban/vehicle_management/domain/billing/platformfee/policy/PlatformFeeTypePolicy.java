package com.ban.vehicle_management.domain.billing.platformfee.policy;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class PlatformFeeTypePolicy {

    public String normalizeFeeCode(String feeCode) {
        String normalized = TextValidationUtils.normalizeRequiredText(feeCode, "feeCode", 100)
                .toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9_]+$")) {
            throw new BadRequestException("feeCode must contain only A-Z, 0-9 and underscore");
        }
        return normalized;
    }

    public void initializeNewType(PlatformFeeType feeType) {
        requireType(feeType);
        feeType.setFeeCode(normalizeFeeCode(feeType.getFeeCode()));
        feeType.setName(TextValidationUtils.normalizeRequiredText(feeType.getName(), "name", 200));
        feeType.setDescription(TextValidationUtils.normalizeNullableText(feeType.getDescription(), "description", 1000));
        if (feeType.getTransactionType() == null) {
            feeType.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        }
        if (feeType.getCalculationType() == null) {
            feeType.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        }
        requireSupportedTransactionType(feeType.getTransactionType());
        requireSupportedCalculationType(feeType.getCalculationType());
        requireFeeBasis(feeType.getFeeBasis());
        requirePartnerChargedTo(feeType.getChargedTo());
        feeType.setStatus(PlatformFeeTypeStatus.DRAFT);
        feeType.setArchivedAt(null);
        feeType.setArchivedBy(null);
        validateState(feeType);
    }

    public void updateDraftType(PlatformFeeType existing, PlatformFeeType changes) {
        requireType(existing);
        requireType(changes);
        requireStatus(existing, PlatformFeeTypeStatus.DRAFT, "Only DRAFT fee type can be updated");
        existing.setName(TextValidationUtils.normalizeRequiredText(changes.getName(), "name", 200));
        existing.setDescription(
                TextValidationUtils.normalizeNullableText(changes.getDescription(), "description", 1000));
        requireFeeBasis(changes.getFeeBasis());
        existing.setFeeBasis(changes.getFeeBasis());
        existing.setStackable(changes.isStackable());
        validateState(existing);
    }

    public void activate(PlatformFeeType feeType) {
        requireType(feeType);
        if (PlatformFeeTypeStatus.ARCHIVED.equals(feeType.getStatus())) {
            throw new ConflictException("Archived fee type cannot be activated");
        }
        if (PlatformFeeTypeStatus.ACTIVE.equals(feeType.getStatus())) {
            return;
        }
        if (!PlatformFeeTypeStatus.DRAFT.equals(feeType.getStatus())
                && !PlatformFeeTypeStatus.INACTIVE.equals(feeType.getStatus())) {
            throw new ConflictException("Only DRAFT or INACTIVE fee type can be activated");
        }
        feeType.setStatus(PlatformFeeTypeStatus.ACTIVE);
        validateState(feeType);
    }

    public void deactivate(PlatformFeeType feeType) {
        requireType(feeType);
        if (!PlatformFeeTypeStatus.ACTIVE.equals(feeType.getStatus())) {
            throw new ConflictException("Only ACTIVE fee type can be deactivated");
        }
        feeType.setStatus(PlatformFeeTypeStatus.INACTIVE);
        validateState(feeType);
    }

    public void archive(PlatformFeeType feeType) {
        requireType(feeType);
        if (PlatformFeeTypeStatus.ARCHIVED.equals(feeType.getStatus())) {
            return;
        }
        if (!PlatformFeeTypeStatus.DRAFT.equals(feeType.getStatus())
                && !PlatformFeeTypeStatus.INACTIVE.equals(feeType.getStatus())) {
            throw new ConflictException("Only DRAFT or INACTIVE fee type can be archived");
        }
        feeType.setStatus(PlatformFeeTypeStatus.ARCHIVED);
        validateState(feeType);
    }

    public void validateState(PlatformFeeType feeType) {
        requireType(feeType);
        requireField(feeType.getPlatformFeeTypeId(), "platformFeeTypeId");
        requireField(feeType.getFeeCode(), "feeCode");
        requireField(feeType.getName(), "name");
        requireField(feeType.getTransactionType(), "transactionType");
        requireField(feeType.getCalculationType(), "calculationType");
        requireField(feeType.getFeeBasis(), "feeBasis");
        requireField(feeType.getChargedTo(), "chargedTo");
        requireField(feeType.getStatus(), "status");
        requireSupportedTransactionType(feeType.getTransactionType());
        requireSupportedCalculationType(feeType.getCalculationType());
    }

    public void requirePartnerChargedTo(PlatformFeeChargedTo chargedTo) {
        if (chargedTo == null) {
            throw new BadRequestException("chargedTo must not be null");
        }
        if (!PlatformFeeChargedTo.PARTNER.equals(chargedTo)) {
            throw new BadRequestException("Only PARTNER chargedTo is supported in this phase");
        }
    }

    private void requireSupportedTransactionType(PlatformFeeTransactionType transactionType) {
        if (!PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE.equals(transactionType)) {
            throw new BadRequestException("Only SUBSCRIPTION_PURCHASE transactionType is supported");
        }
    }

    private void requireSupportedCalculationType(PlatformFeeCalculationType calculationType) {
        if (!PlatformFeeCalculationType.PERCENTAGE.equals(calculationType)) {
            throw new BadRequestException("Only PERCENTAGE calculationType is supported");
        }
    }

    private void requireFeeBasis(PlatformFeeBasis feeBasis) {
        if (feeBasis == null) {
            throw new BadRequestException("feeBasis must not be null");
        }
    }

    private void requireStatus(PlatformFeeType feeType, PlatformFeeTypeStatus expected, String message) {
        if (!expected.equals(feeType.getStatus())) {
            throw new ConflictException(message);
        }
    }

    private void requireType(PlatformFeeType feeType) {
        requireField(feeType, "platformFeeType");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
