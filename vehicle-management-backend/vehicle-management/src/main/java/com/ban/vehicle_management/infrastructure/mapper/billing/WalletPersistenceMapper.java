package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.FinancialTransactionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.LedgerAccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.LedgerEntryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletAdjustmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletEntity;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.domain.billing.wallet.model.PartnerBankAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.PayoutRequest;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletTopupOrderEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.RevenueAllocationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.VoucherFinancialTermEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PartnerBankAccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PayoutRequestEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletPersistenceMapper {

    WalletEntity toEntity(Wallet domain);

    Wallet toDomain(WalletEntity entity);

    FinancialTransactionEntity toEntity(FinancialTransaction domain);

    FinancialTransaction toDomain(FinancialTransactionEntity entity);

    LedgerAccountEntity toEntity(LedgerAccount domain);

    LedgerAccount toDomain(LedgerAccountEntity entity);

    LedgerEntryEntity toEntity(LedgerEntry domain);

    LedgerEntry toDomain(LedgerEntryEntity entity);

    WalletAdjustmentEntity toEntity(WalletAdjustment domain);

    WalletAdjustment toDomain(WalletAdjustmentEntity entity);

    WalletTopupOrderEntity toEntity(WalletTopupOrder domain);

    WalletTopupOrder toDomain(WalletTopupOrderEntity entity);

    RevenueAllocationEntity toEntity(RevenueAllocation domain);

    RevenueAllocation toDomain(RevenueAllocationEntity entity);

    VoucherFinancialTermEntity toEntity(VoucherFinancialTerm domain);

    VoucherFinancialTerm toDomain(VoucherFinancialTermEntity entity);

    PartnerBankAccountEntity toEntity(PartnerBankAccount domain);

    PartnerBankAccount toDomain(PartnerBankAccountEntity entity);

    PayoutRequestEntity toEntity(PayoutRequest domain);

    PayoutRequest toDomain(PayoutRequestEntity entity);
}
