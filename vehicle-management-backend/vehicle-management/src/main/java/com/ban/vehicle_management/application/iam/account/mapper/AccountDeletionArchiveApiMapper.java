package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedIdentityMatch;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionAuditResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.ClosedOrganizationArchiveResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.DeletedAccountArchiveResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.DeletedIdentityMatchResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountDeletionArchiveApiMapper {
    DeletedAccountArchiveResponse toResponse(DeletedAccountArchiveResult result);
    List<DeletedAccountArchiveResponse> toDeletedAccountResponses(List<DeletedAccountArchiveResult> results);
    ClosedOrganizationArchiveResponse toResponse(ClosedOrganizationArchiveResult result);
    List<ClosedOrganizationArchiveResponse> toClosedOrganizationResponses(
            List<ClosedOrganizationArchiveResult> results);
    AccountDeletionAuditResponse toResponse(AccountDeletionAuditResult result);
    List<DeletedIdentityMatchResponse> toIdentityResponses(List<DeletedIdentityMatch> results);
}
