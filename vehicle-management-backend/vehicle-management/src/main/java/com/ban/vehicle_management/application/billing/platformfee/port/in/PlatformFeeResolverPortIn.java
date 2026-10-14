package com.ban.vehicle_management.application.billing.platformfee.port.in;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.time.Instant;

public interface PlatformFeeResolverPortIn {

    PlatformFeeResolution resolveFees(PlatformFeeTransactionType transactionType,
            Instant effectiveAt, long grossAmount, long customerPaidAmount);
}
