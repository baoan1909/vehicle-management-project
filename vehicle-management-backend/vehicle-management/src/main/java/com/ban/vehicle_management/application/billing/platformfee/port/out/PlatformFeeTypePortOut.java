package com.ban.vehicle_management.application.billing.platformfee.port.out;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformFeeTypePortOut {

    PlatformFeeType save(PlatformFeeType feeType);

    Optional<PlatformFeeType> findById(UUID feeTypeId);

    Optional<PlatformFeeType> findByFeeCode(String feeCode);

    PlatformFeePageResult findAll(String keyword, PlatformFeeTypeStatus status,
            PlatformFeeTransactionType transactionType, PlatformFeeBasis feeBasis, int page, int size);

    List<PlatformFeeType> findActiveByTransactionType(PlatformFeeTransactionType transactionType);

    void lockById(UUID feeTypeId);

    void lockByTransactionType(PlatformFeeTransactionType transactionType);

    boolean existsByFeeCode(String feeCode);
}
