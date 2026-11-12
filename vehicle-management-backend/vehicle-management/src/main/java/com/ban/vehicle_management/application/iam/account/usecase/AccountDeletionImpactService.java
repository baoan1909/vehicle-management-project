package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;
import com.ban.vehicle_management.domain.iam.account.model.OrganizationClosureImpact;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountDeletionImpactService {
    private static final List<String> ARCHIVE_DATA = List.of(
            "Tư cách thành viên và phạm vi truy cập tổ chức",
            "Bãi xe, cấu hình vận hành và phân công",
            "Chat nội bộ thuộc tổ chức",
            "Tài liệu AI TENANT_PRIVATE thuộc tổ chức");
    private static final List<String> RETAIN_DATA = List.of(
            "Hóa đơn, thanh toán, payout và sổ cái",
            "Lịch sử phiên gửi xe và bằng chứng có retention",
            "Audit log và lịch sử trạng thái");

    private final AccountDeletionPortOut accountDeletionPort;
    private final OrganizationClosurePortOut organizationClosurePort;
    private final CustomerAccountDeletionImpactService customerImpactService;

    public AccountDeletionImpactService(
            AccountDeletionPortOut accountDeletionPort,
            OrganizationClosurePortOut organizationClosurePort,
            CustomerAccountDeletionImpactService customerImpactService) {
        this.accountDeletionPort = accountDeletionPort;
        this.organizationClosurePort = organizationClosurePort;
        this.customerImpactService = customerImpactService;
    }

    public AccountDeletionImpact analyze(UUID accountId, boolean lockOrganizations) {
        List<OrganizationClosureImpact> scopes = organizationClosurePort
                .findClosureImpacts(accountId, lockOrganizations);
        List<UUID> organizationIds = scopes.stream()
                .map(OrganizationClosureImpact::organizationId).toList();
        List<AccountDeletionBlocker> accountBlockers = accountDeletionPort
                .findDeletionBlockers(accountId, organizationIds);
        var customerDecision = customerImpactService.analyze(accountId, null);
        accountBlockers = java.util.stream.Stream.concat(
                accountBlockers.stream(), customerDecision.blockers().stream()).toList();
        List<AccountDeletionBlocker> organizationBlockers = organizationClosurePort
                .findBlockers(organizationIds);
        return new AccountDeletionImpact(
                accountBlockers,
                organizationBlockers,
                customerDecision.automaticActions(),
                scopes,
                scopes.size(),
                scopes.stream().mapToLong(OrganizationClosureImpact::affectedParkingLotCount).sum(),
                scopes.stream().mapToLong(OrganizationClosureImpact::affectedMemberCount).sum(),
                organizationIds.isEmpty() ? List.of() : ARCHIVE_DATA,
                organizationIds.isEmpty() ? List.of() : RETAIN_DATA,
                !organizationIds.isEmpty());
    }
}
