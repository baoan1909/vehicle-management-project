package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.port.out.VnpayGatewayPortOut;
import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletTopupPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.billing.wallet.model.LedgerAccount;
import com.ban.vehicle_management.domain.billing.wallet.model.Wallet;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerAccountType;
import com.ban.vehicle_management.shared.enumeration.billing.WalletStatus;
import com.ban.vehicle_management.shared.enumeration.billing.WalletTopupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletTopupUseCaseImplTest {

    @Mock WalletPortOut walletPortOut;
    @Mock WalletTopupPortOut topupPortOut;
    @Mock LedgerPortOut ledgerPortOut;
    @Mock VnpayGatewayPortOut vnpayGatewayPortOut;
    @Mock WalletAccessGuard walletAccessGuard;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock NotificationPortIn notificationPortIn;

    private WalletTopupUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new WalletTopupUseCaseImpl(
                walletPortOut, topupPortOut, ledgerPortOut, vnpayGatewayPortOut,
                walletAccessGuard, currentAccountPortIn, notificationPortIn,
                "https://app.example.test/customer/wallet/topup-result");
    }

    @Test
    void createTopupReturnsExistingOrderForSameIdempotencyKey() {
        WalletTopupOrder existing = pendingOrder();
        when(topupPortOut.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        assertSame(existing, useCase.createTopup(BigDecimal.valueOf(10000), "same-key", "127.0.0.1"));

        verify(topupPortOut).lockIdempotencyKey("same-key");
        verifyNoInteractions(vnpayGatewayPortOut, walletPortOut);
    }

    @Test
    void invalidIpnSignatureDoesNotMutateMoney() {
        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(new VnpayCallbackData(
                false, null, "TXN", BigDecimal.valueOf(10000), "00", "00",
                null, null, null, null));

        var result = useCase.processIpn(new VnpayCallbackCommand(Map.of("vnp_TxnRef", "TXN")));

        assertEquals("97", result.responseCode());
        verifyNoInteractions(walletPortOut, ledgerPortOut);
        verify(topupPortOut, never()).save(any());
    }

    @Test
    void returnUrlOnlyReportsLocalStatusAndNeverCreditsWallet() {
        WalletTopupOrder order = pendingOrder();
        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(successCallback(order));
        when(topupPortOut.findByTransactionRef(order.getTransactionRef())).thenReturn(Optional.of(order));

        var result = useCase.verifyReturn(new VnpayCallbackCommand(Map.of(
                "vnp_TxnRef", order.getTransactionRef())));

        assertEquals(com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus.PENDING,
                result.paymentStatus());
        verifyNoInteractions(walletPortOut, ledgerPortOut);
        verify(topupPortOut, never()).save(any());
    }

    @Test
    void successfulIpnCreditsWalletAndPostsOneBalancedTransaction() {
        WalletTopupOrder order = pendingOrder();
        Wallet wallet = new Wallet();
        wallet.setWalletId(order.getWalletId());
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(successCallback(order));
        when(topupPortOut.findByTransactionRefForUpdate(order.getTransactionRef()))
                .thenReturn(Optional.of(order));
        when(walletPortOut.findByIdForUpdate(order.getWalletId())).thenReturn(Optional.of(wallet));
        when(ledgerPortOut.findTransactionByIdempotencyKey("TOPUP-" + order.getTopupOrderId()))
                .thenReturn(Optional.empty());
        when(ledgerPortOut.findAccountByCode(LedgerAccountType.PLATFORM_CASH_CLEARING.name()))
                .thenReturn(Optional.of(account(LedgerAccountType.PLATFORM_CASH_CLEARING)));
        when(ledgerPortOut.findAccountByCode(LedgerAccountType.CUSTOMER_WALLET_LIABILITY.name()))
                .thenReturn(Optional.of(account(LedgerAccountType.CUSTOMER_WALLET_LIABILITY)));
        when(walletPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(topupPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.processIpn(new VnpayCallbackCommand(Map.of(
                "vnp_TxnRef", order.getTransactionRef())));

        assertEquals("00", result.responseCode());
        assertEquals(BigDecimal.valueOf(10000), wallet.getAvailableBalance());
        assertEquals(WalletTopupStatus.COMPLETED, order.getStatus());
        verify(ledgerPortOut).postBalancedTransaction(any(), any());
    }

    private WalletTopupOrder pendingOrder() {
        WalletTopupOrder order = new WalletTopupOrder();
        order.setTopupOrderId(UUID.randomUUID());
        order.setWalletId(UUID.randomUUID());
        order.setAmount(BigDecimal.valueOf(10000));
        order.setCurrency("VND");
        order.setStatus(WalletTopupStatus.PENDING);
        order.setTransactionRef("WLT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setExpiresAt(Instant.now().plusSeconds(900));
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setCreatedAt(Instant.now());
        return order;
    }

    private VnpayCallbackData successCallback(WalletTopupOrder order) {
        return new VnpayCallbackData(
                true, "TEST", order.getTransactionRef(), order.getAmount(), "00", "00",
                "provider-transaction", "NCB", "ATM", Instant.now());
    }

    private LedgerAccount account(LedgerAccountType type) {
        LedgerAccount account = new LedgerAccount();
        account.setLedgerAccountId(UUID.randomUUID());
        account.setAccountCode(type.name());
        account.setAccountType(type);
        account.setCurrency("VND");
        return account;
    }
}
