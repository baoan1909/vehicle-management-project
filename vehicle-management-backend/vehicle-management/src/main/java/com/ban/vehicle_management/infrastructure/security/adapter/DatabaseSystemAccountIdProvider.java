package com.ban.vehicle_management.infrastructure.security.adapter;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseSystemAccountIdProvider implements SystemAccountIdProvider {

    private static final String SYSTEM_USERNAME = "SYSTEM";
    private final AccountRepository accountRepository;
    private volatile UUID cachedSystemAccountId;

    public DatabaseSystemAccountIdProvider(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UUID getSystemAccountId() {
        UUID cached = cachedSystemAccountId;
        if (cached != null) {
            return cached;
        }

        UUID resolved = accountRepository.findByUsername(SYSTEM_USERNAME)
                .map(account -> account.getAccountId())
                .orElse(null);

        if (resolved == null) {
            throw new IllegalStateException("SYSTEM account is not found.");
        }

        cachedSystemAccountId = resolved;
        return resolved;
    }
}

