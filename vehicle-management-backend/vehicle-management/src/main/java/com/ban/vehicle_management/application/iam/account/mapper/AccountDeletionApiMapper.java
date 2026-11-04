package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionResponse;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionApiMapper {

    public AccountDeletionResponse toResponse(AccountDeletionRequest request, Instant now) {
        if (request == null) return null;
        return new AccountDeletionResponse(request.requestId(), request.status(), request.requestedAt(),
                request.scheduledDeletionAt(), request.cancelledAt(), request.completedAt(),
                request.cancellableAt(now));
    }
}
