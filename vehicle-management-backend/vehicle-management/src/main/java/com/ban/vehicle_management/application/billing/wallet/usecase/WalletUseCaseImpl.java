package com.ban.vehicle_management.application.billing.wallet.usecase;

import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.in.WalletPortIn;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.FinancialTransaction;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.policy.WalletPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.WalletOwnerType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletUseCaseImpl implements WalletPortIn {

    private static final String CURRENCY = "VND";

    private final WalletPortOut walletPortOut;
    private final LedgerPortOut ledgerPortOut;
    private final WalletAccessGuard walletAccessGuard;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CustomerPortOut customerPortOut;
    private final OrganizationPortOut organizationPortOut;
    private final WalletPolicy walletPolicy = new WalletPolicy();

    public WalletUseCaseImpl(
            WalletPortOut walletPortOut,
            LedgerPortOut ledgerPortOut,
            WalletAccessGuard walletAccessGuard,
            CurrentAccountPortIn currentAccountPortIn,
            CustomerPortOut customerPortOut,
            OrganizationPortOut organizationPortOut) {
        this.walletPortOut = walletPortOut;
        this.ledgerPortOut = ledgerPortOut;
        this.walletAccessGuard = walletAccessGuard;
        this.currentAccountPortIn = currentAccountPortIn;
        this.customerPortOut = customerPortOut;
        this.organizationPortOut = organizationPortOut;
    }

    @Override
    @Transactional
    public Wallet provisionCustomerWallet(UUID customerId) {
        if (customerId == null) {
            throw new BadRequestException("customerId must not be null");
        }
        var customer = customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!CustomerStatus.ACTIVE.equals(customer.getStatus())
                || !CustomerApprovalStatus.APPROVED.equals(customer.getApprovalStatus())) {
            throw new BadRequestException("Customer must be APPROVED and ACTIVE before wallet provisioning");
        }
        var existing = walletPortOut.findCustomerWallet(customerId, CURRENCY, WalletPurpose.PERSONAL);
        if (existing.isPresent()) {
            return existing.get();
        }
        Wallet wallet = newWallet(
                WalletOwnerType.CUSTOMER, customerId, null, WalletPurpose.PERSONAL);
        try {
            return walletPortOut.save(wallet);
        } catch (DataIntegrityViolationException exception) {
            return walletPortOut
                    .findCustomerWallet(customerId, CURRENCY, WalletPurpose.PERSONAL)
                    .orElseThrow(() -> new ConflictException("Wallet already exists"));
        }
    }

    @Override
    @Transactional
    public Wallet provisionPartnerWallet(UUID organizationId) {
        if (organizationId == null) {
            throw new BadRequestException("organizationId must not be null");
        }
        var organization = organizationPortOut.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        if (!OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
            throw new BadRequestException("Organization must be ACTIVE before wallet provisioning");
        }
        var existing = walletPortOut.findOrganizationWallet(
                organizationId, CURRENCY, WalletPurpose.ORGANIZATION_SETTLEMENT);
        if (existing.isPresent()) {
            return existing.get();
        }
        Wallet wallet = newWallet(
                WalletOwnerType.ORGANIZATION, null, organizationId, WalletPurpose.ORGANIZATION_SETTLEMENT);
        try {
            return walletPortOut.save(wallet);
        } catch (DataIntegrityViolationException exception) {
            return walletPortOut
                    .findOrganizationWallet(organizationId, CURRENCY, WalletPurpose.ORGANIZATION_SETTLEMENT)
                    .orElseThrow(() -> new ConflictException("Wallet already exists"));
        }
    }

    @Override
    @Transactional
    public Wallet getMyWallet() {
        currentAccountPortIn.requirePermission(WalletAccessGuard.READ_OWN);
        UUID customerId = walletAccessGuard.resolveCurrentApprovedCustomerId();
        return walletPortOut
                .findCustomerWallet(customerId, CURRENCY, WalletPurpose.PERSONAL)
                .orElseGet(() -> provisionCustomerWallet(customerId));
    }

    @Override
    @Transactional
    public Wallet getCurrentPartnerWallet() {
        currentAccountPortIn.requirePermission(WalletAccessGuard.READ_PARTNER);
        UUID organizationId = walletAccessGuard.resolveCurrentPartnerOrganizationId();
        return walletPortOut
                .findOrganizationWallet(organizationId, CURRENCY, WalletPurpose.ORGANIZATION_SETTLEMENT)
                .orElseGet(() -> provisionPartnerWallet(organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletById(UUID walletId) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.READ_ALL);
        Wallet wallet = walletPortOut.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        return wallet;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Wallet> listWallets(int page, int size) {
        currentAccountPortIn.requirePermission(WalletAccessGuard.READ_ALL);
        return walletPortOut.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialTransaction> getMyTransactions(int page, int size) {
        Wallet wallet = getMyWallet();
        walletAccessGuard.ensureCanReadTransactions(wallet);
        return ledgerPortOut.findTransactionsByWallet(wallet.getWalletId(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialTransaction> getCurrentPartnerTransactions(int page, int size) {
        Wallet wallet = getCurrentPartnerWallet();
        walletAccessGuard.ensureCanReadTransactions(wallet);
        return ledgerPortOut.findTransactionsByWallet(wallet.getWalletId(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialTransaction> getWalletTransactions(UUID walletId, int page, int size) {
        Wallet wallet = getWalletById(walletId);
        walletAccessGuard.ensureCanReadTransactions(wallet);
        return ledgerPortOut.findTransactionsByWallet(walletId, page, size);
    }

    private Wallet newWallet(
            WalletOwnerType ownerType, UUID customerId, UUID organizationId, WalletPurpose purpose) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setOwnerType(ownerType);
        wallet.setCustomerId(customerId);
        wallet.setOrganizationId(organizationId);
        wallet.setWalletPurpose(purpose);
        wallet.setCurrency(CURRENCY);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setPendingBalance(BigDecimal.ZERO);
        wallet.setHeldBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setCreatedAt(Instant.now());
        try {
            wallet.setCreatedBy(currentAccountPortIn.getCurrentAccountId().orElse(null));
        } catch (Exception ignored) {
            wallet.setCreatedBy(null);
        }
        walletPolicy.validateState(wallet);
        return wallet;
    }
}
