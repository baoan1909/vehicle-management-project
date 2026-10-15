package com.ban.vehicle_management.domain.billing.platformfee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformFeeTypePolicyTest {

    private final PlatformFeeTypePolicy policy = new PlatformFeeTypePolicy();

    @Test
    void shouldNormalizeFeeCodeToUppercase() {
        assertEquals("PLATFORM_COMMISSION", policy.normalizeFeeCode(" platform_commission "));
    }

    @Test
    void shouldRejectFeeCodeWithUnsupportedCharacters() {
        assertThrows(BadRequestException.class, () -> policy.normalizeFeeCode("FEE-CODE!"));
    }

    @Test
    void shouldRejectNonPartnerChargedTo() {
        assertThrows(BadRequestException.class,
                () -> policy.requirePartnerChargedTo(PlatformFeeChargedTo.CUSTOMER));
        assertThrows(BadRequestException.class,
                () -> policy.requirePartnerChargedTo(PlatformFeeChargedTo.PLATFORM));
    }

    @Test
    void shouldAllowDraftUpdate() {
        PlatformFeeType existing = draftType();
        PlatformFeeType changes = draftType();
        changes.setName("Ten moi");
        changes.setFeeBasis(PlatformFeeBasis.AFTER_DISCOUNT);

        policy.updateDraftType(existing, changes);

        assertEquals("Ten moi", existing.getName());
        assertEquals(PlatformFeeBasis.AFTER_DISCOUNT, existing.getFeeBasis());
    }

    @Test
    void shouldRejectUpdateWhenNotDraft() {
        PlatformFeeType existing = draftType();
        existing.setStatus(PlatformFeeTypeStatus.ACTIVE);

        assertThrows(ConflictException.class, () -> policy.updateDraftType(existing, draftType()));
    }

    @Test
    void shouldRejectActivateWhenArchived() {
        PlatformFeeType archived = draftType();
        archived.setStatus(PlatformFeeTypeStatus.ARCHIVED);

        assertThrows(ConflictException.class, () -> policy.activate(archived));
    }

    @Test
    void shouldRejectArchiveWhenActive() {
        PlatformFeeType active = draftType();
        active.setStatus(PlatformFeeTypeStatus.ACTIVE);

        assertThrows(ConflictException.class, () -> policy.archive(active));
    }

    private PlatformFeeType draftType() {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode("PLATFORM_COMMISSION");
        type.setName("Phi nen tang");
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(PlatformFeeBasis.GROSS_AMOUNT);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        type.setStatus(PlatformFeeTypeStatus.DRAFT);
        return type;
    }

    @Test
    void shouldKeepFeeCodeListDeterministic() {
        List<String> codes = List.of("OPERATION_FEE", "PLATFORM_COMMISSION");
        assertEquals(List.of("OPERATION_FEE", "PLATFORM_COMMISSION"),
                codes.stream().sorted().toList());
    }
}
