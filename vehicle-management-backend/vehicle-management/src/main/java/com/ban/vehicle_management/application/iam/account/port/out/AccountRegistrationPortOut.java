package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.domain.iam.account.model.Account;

import java.util.Optional;

public interface AccountRegistrationPortOut {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<String> findKeycloakUserIdByEmail(String email);

    Account registerAccount(RegisterAccountCommand command, String keycloakUserId);
}
