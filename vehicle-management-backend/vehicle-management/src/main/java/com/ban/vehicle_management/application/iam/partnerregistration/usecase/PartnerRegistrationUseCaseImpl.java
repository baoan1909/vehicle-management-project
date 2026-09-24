package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.ProvisionedAccountPortIn;
import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerRegistrationPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
public class PartnerRegistrationUseCaseImpl implements PartnerRegistrationPortIn {
    public static final String REQUEST_TYPE = "PARTNER_REGISTRATION";
    public static final String TARGET_SCHEMA = "iam";
    public static final String TARGET_TABLE = "organizations";
    private final PartnerRegistrationPortOut partnerRegistrationPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ProvisionedAccountPortIn provisionedAccountPortIn;
    private final OrganizationPortIn organizationPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final PartnerRegistrationPolicy partnerRegistrationPolicy = new PartnerRegistrationPolicy();
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();

    public PartnerRegistrationUseCaseImpl(
            PartnerRegistrationPortOut partnerRegistrationPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            ProvisionedAccountPortIn provisionedAccountPortIn,
            OrganizationPortIn organizationPortIn,
            OrganizationPortOut organizationPortOut
    ) {
        this.partnerRegistrationPortOut = partnerRegistrationPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.provisionedAccountPortIn = provisionedAccountPortIn;
        this.organizationPortIn = organizationPortIn;
        this.organizationPortOut = organizationPortOut;
    }

    @Override
    @Transactional
    public PartnerRegistrationResult submitRegistration(CreatePartnerRegistrationCommand command) {
        CreatePartnerRegistrationCommand normalized = partnerRegistrationPolicy.normalize(command);
        if (partnerRegistrationPortOut.existsPendingByEmail(normalized.email())) {
            throw new ConflictException("A partner registration with this email is already pending");
        }
        if (partnerRegistrationPortOut.existsPendingByOrganizationCode(normalized.organizationCode())) {
            throw new ConflictException("A partner registration with this organization code is already pending");
        }
        ApprovalRequest approval = new ApprovalRequest();
        approval.setApprovalRequestId(UUID.randomUUID());
        approval.setRequestType(REQUEST_TYPE);
        approval.setTargetSchema(TARGET_SCHEMA);
        approval.setTargetTable(TARGET_TABLE);
        approval.setTargetId(UUID.randomUUID());
        approval.setStatus(ApprovalRequestStatus.PENDING);
        approval.setRequestData(Map.of(
                "organizationCode", normalized.organizationCode(), "organizationName", normalized.organizationName(),
                "representativeName", normalized.representativeName(), "email", normalized.email(),
                "phoneNumber", normalized.phoneNumber(), "address", normalized.address(),
                "expectedParkingLotCount", normalized.expectedParkingLotCount().toString(),
                "parkingOperationDescription", normalized.parkingOperationDescription() == null ? "" : normalized.parkingOperationDescription()
        ));
        approvalRequestPolicy.initialize(approval);
        partnerRegistrationPortOut.save(approval);
        return new PartnerRegistrationResult(approval.getApprovalRequestId(), normalized.organizationCode(), normalized.organizationName(), normalized.representativeName(), normalized.email(), normalized.phoneNumber(), normalized.address(), normalized.expectedParkingLotCount(), normalized.parkingOperationDescription(), ApprovalRequestStatus.PENDING, null, approval.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerRegistrationResult> getPartnerRegistrations(ApprovalRequestStatus status) {
        currentAccountPortIn.requirePermission("ORGANIZATION_READ_ALL");
        ensureSystemAdmin();
        return partnerRegistrationPortOut.findAll(status);
    }

    @Override
    @Transactional
    public PartnerRegistrationResult approveRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command) {
        currentAccountPortIn.requirePermission("ORGANIZATION_CREATE_ALL");
        ensureSystemAdmin();
        ApprovalRequest approvalRequest = getPendingRegistration(approvalRequestId);
        String organizationCode = approvalRequest.getRequestData().get("organizationCode");
        if (organizationPortOut.existsByCode(organizationCode)) {
            throw new ConflictException("Organization code already exists");
        }

        Account partnerAdminAccount = new Account();
        partnerAdminAccount.setUsername(approvalRequest.getRequestData().get("email"));
        partnerAdminAccount.setEmail(approvalRequest.getRequestData().get("email"));
        var provisionedAccount = provisionedAccountPortIn.createProvisionedAccount(new CreateProvisionedAccountCommand(
                partnerAdminAccount,
                null,
                AdminProvisionableAccountRoleCode.PARTNER_ADMIN,
                approvalRequest.getRequestData().get("representativeName")
        ));

        Organization organization = new Organization();
        organization.setCode(organizationCode);
        organization.setName(approvalRequest.getRequestData().get("organizationName"));
        organization.setAddress(approvalRequest.getRequestData().get("address"));
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization createdOrganization = organizationPortIn.createOrganization(
                organization,
                provisionedAccount.account().accountId()
        );

        approvalRequest.setDecisionData(Map.of("organizationId", createdOrganization.getOrganizationId().toString()));
        approvalRequestPolicy.approve(
                approvalRequest,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now(),
                normalizeReviewNote(command)
        );
        partnerRegistrationPortOut.save(approvalRequest);
        return toResult(approvalRequest);
    }

    @Override
    @Transactional
    public PartnerRegistrationResult rejectRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command) {
        currentAccountPortIn.requirePermission("ORGANIZATION_CREATE_ALL");
        ensureSystemAdmin();
        ApprovalRequest approvalRequest = getPendingRegistration(approvalRequestId);
        approvalRequestPolicy.reject(approvalRequest, normalizeReviewNote(command));
        partnerRegistrationPortOut.save(approvalRequest);
        return toResult(approvalRequest);
    }

    private ApprovalRequest getPendingRegistration(UUID approvalRequestId) {
        if (approvalRequestId == null) {
            throw new NotFoundException("Partner registration not found");
        }
        ApprovalRequest approvalRequest = partnerRegistrationPortOut.findById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Partner registration not found"));
        if (approvalRequest.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Partner registration has already been reviewed");
        }
        return approvalRequest;
    }

    private String normalizeReviewNote(ReviewPartnerRegistrationCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private void ensureSystemAdmin() {
        if (!OrganizationAccessGuard.SYSTEM_ADMIN.equals(currentAccountPortIn.getCurrentAccountOrThrow().roleCode())) {
            throw new AccessDeniedException("Current account is not a system administrator");
        }
    }

    private PartnerRegistrationResult toResult(ApprovalRequest approvalRequest) {
        Map<String, String> data = approvalRequest.getRequestData();
        return new PartnerRegistrationResult(
                approvalRequest.getApprovalRequestId(),
                data.get("organizationCode"),
                data.get("organizationName"),
                data.get("representativeName"),
                data.get("email"),
                data.get("phoneNumber"),
                data.get("address"),
                Integer.valueOf(data.get("expectedParkingLotCount")),
                data.get("parkingOperationDescription"),
                approvalRequest.getStatus(),
                approvalRequest.getNote(),
                approvalRequest.getCreatedAt()
        );
    }
}
