package com.ban.vehicle_management.application.billing.wallet.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.wallet.authorization.WalletAccessGuard;
import com.ban.vehicle_management.application.billing.wallet.port.out.LedgerPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletAdjustmentPortOut;
import com.ban.vehicle_management.application.billing.wallet.port.out.WalletPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import com.ban.vehicle_management.shared.enumeration.billing.WalletAdjustmentStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class WalletAdminUseCaseImplTest {

    @Mock WalletPortOut walletPortOut;
    @Mock LedgerPortOut ledgerPortOut;
    @Mock WalletAdjustmentPortOut adjustmentPortOut;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @InjectMocks WalletAdminUseCaseImpl useCase;

    @Test
    void makerCannotApproveOwnAdjustment() {
        UUID makerId = UUID.randomUUID();
        UUID adjustmentId = UUID.randomUUID();
        WalletAdjustment adjustment = new WalletAdjustment();
        adjustment.setWalletAdjustmentId(adjustmentId);
        adjustment.setRequestedBy(makerId);
        adjustment.setStatus(WalletAdjustmentStatus.PENDING);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(makerId);
        when(adjustmentPortOut.findByIdForUpdate(adjustmentId)).thenReturn(Optional.of(adjustment));

        assertThrows(AccessDeniedException.class, () -> useCase.approveAdjustment(adjustmentId));
        verifyNoInteractions(walletPortOut, ledgerPortOut);
    }

    @Test
    void accountWithoutAdjustmentPermissionCannotCreateRequest() {
        doThrow(new AccessDeniedException("denied"))
                .when(currentAccountPortIn)
                .requirePermission(WalletAccessGuard.ADJUST_REQUEST_ALL);

        assertThrows(AccessDeniedException.class, () -> useCase.requestAdjustment(
                UUID.randomUUID(), BigDecimal.valueOf(1000), "CREDIT", "test"));
        verifyNoInteractions(walletPortOut, ledgerPortOut, adjustmentPortOut);
    }
}
