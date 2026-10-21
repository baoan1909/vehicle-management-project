package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerPostingPolicyTest {

    private final LedgerPostingPolicy policy = new LedgerPostingPolicy();

    private LedgerEntry entry(LedgerEntrySide side, String amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setLedgerEntryId(UUID.randomUUID());
        entry.setLedgerAccountId(UUID.randomUUID());
        entry.setEntrySide(side);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }

    @Test
    void balancedEntriesPass() {
        assertDoesNotThrow(() -> policy.requireBalanced(List.of(
                entry(LedgerEntrySide.DEBIT, "180000"),
                entry(LedgerEntrySide.CREDIT, "171000"),
                entry(LedgerEntrySide.CREDIT, "8100"),
                entry(LedgerEntrySide.CREDIT, "900"))));
    }

    @Test
    void unbalancedEntriesFail() {
        assertThrows(Exception.class, () -> policy.requireBalanced(List.of(
                entry(LedgerEntrySide.DEBIT, "180000"),
                entry(LedgerEntrySide.CREDIT, "170000"))));
    }

    @Test
    void emptyEntriesFail() {
        assertThrows(Exception.class, () -> policy.requireBalanced(List.of()));
    }
}
