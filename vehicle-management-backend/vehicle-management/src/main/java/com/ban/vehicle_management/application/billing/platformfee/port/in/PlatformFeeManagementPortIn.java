package com.ban.vehicle_management.application.billing.platformfee.port.in;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeConfigurationResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeTypeDetailResult;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlatformFeeManagementPortIn {

    PlatformFeeType createFeeType(PlatformFeeType feeType);

    PlatformFeePageResult searchFeeTypes(String keyword, PlatformFeeTypeStatus status,
            PlatformFeeTransactionType transactionType, PlatformFeeBasis feeBasis, int page, int size);

    BigDecimal getMaxPartnerTotalRate();

    PlatformFeeConfigurationResult getConfiguration();

    PlatformFeeConfigurationResult updateConfiguration(BigDecimal maxPartnerTotalRate, long expectedVersion);

    PlatformFeeTypeDetailResult getFeeTypeById(UUID feeTypeId);

    PlatformFeeType updateDraftFeeType(UUID feeTypeId, PlatformFeeType changes);

    PlatformFeeType activateFeeType(UUID feeTypeId);

    PlatformFeeType deactivateFeeType(UUID feeTypeId);

    PlatformFeeType archiveFeeType(UUID feeTypeId);

    PlatformFeeRuleVersion createRuleVersion(UUID feeTypeId, BigDecimal feeRate,
            Instant effectiveFrom, Instant effectiveTo);

    PlatformFeeRuleVersion updateDraftRuleVersion(UUID feeTypeId, UUID versionId,
            BigDecimal feeRate, Instant effectiveFrom, Instant effectiveTo);

    PlatformFeeRuleVersion activateRuleVersion(UUID feeTypeId, UUID versionId);

    PlatformFeeRuleVersion deactivateRuleVersion(UUID feeTypeId, UUID versionId);

    PlatformFeeRuleVersion archiveRuleVersion(UUID feeTypeId, UUID versionId);

    List<PlatformFeeRuleVersion> getVersionHistory(UUID feeTypeId);

    PlatformFeePreviewResult previewFees(PlatformFeeTransactionType transactionType,
            Instant effectiveAt, long grossAmount, long customerPaidAmount);
}
