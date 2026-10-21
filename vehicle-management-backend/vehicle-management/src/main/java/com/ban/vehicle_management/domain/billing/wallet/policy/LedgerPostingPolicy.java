package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.domain.billing.wallet.model.LedgerEntry;
import com.ban.vehicle_management.shared.enumeration.billing.LedgerEntrySide;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Double-entry invariant: SUM(DEBIT) must equal SUM(CREDIT) per financial transaction.
 */
public class LedgerPostingPolicy {

    public void requireBalanced(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new BadRequestException("Ledger entries must not be empty");
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            if (entry == null || entry.getEntrySide() == null || entry.getAmount() == null) {
                throw new BadRequestException("Ledger entry side and amount must not be null");
            }
            if (entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Ledger entry amount must be greater than zero");
            }
            if (LedgerEntrySide.DEBIT.equals(entry.getEntrySide())) {
                debit = debit.add(entry.getAmount());
            } else {
                credit = credit.add(entry.getAmount());
            }
        }
        if (debit.compareTo(credit) != 0) {
            throw new BadRequestException("Ledger transaction is not balanced");
        }
    }
}
