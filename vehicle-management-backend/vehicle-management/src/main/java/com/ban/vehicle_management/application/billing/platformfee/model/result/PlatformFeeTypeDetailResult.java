package com.ban.vehicle_management.application.billing.platformfee.model.result;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import java.util.List;

public record PlatformFeeTypeDetailResult(
        PlatformFeeType feeType,
        PlatformFeeRuleVersion effectiveVersion,
        PlatformFeeRuleVersion upcomingVersion,
        long totalVersions,
        List<PlatformFeeRuleVersion> recentVersions
) {
}
