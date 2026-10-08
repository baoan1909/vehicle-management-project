package com.ban.vehicle_management.application.billing.wallet.mapper;

import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletAdjustmentResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.wallet.response.WalletTransactionResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletApiMapper {

    WalletResponse toResponse(Wallet wallet);

    List<WalletResponse> toResponses(List<Wallet> wallets);

    WalletTransactionResponse toResponse(FinancialTransaction transaction);

    List<WalletTransactionResponse> toTransactionResponses(List<FinancialTransaction> transactions);

    WalletAdjustmentResponse toResponse(WalletAdjustment adjustment);

    List<WalletAdjustmentResponse> toAdjustmentResponses(List<WalletAdjustment> adjustments);
}
