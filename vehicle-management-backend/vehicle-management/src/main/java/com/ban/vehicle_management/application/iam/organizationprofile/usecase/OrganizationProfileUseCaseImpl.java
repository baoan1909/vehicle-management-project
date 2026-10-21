package com.ban.vehicle_management.application.iam.organizationprofile.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.RequestOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.ReviewOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileResult;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileOperationalSummary;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.application.iam.organizationprofile.port.in.OrganizationProfilePortIn;
import com.ban.vehicle_management.application.iam.organizationprofile.port.out.OrganizationProfileApprovalPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Partner Admin proposes profile data; a reviewer alone promotes it to the approved Organization. */
@Service
public class OrganizationProfileUseCaseImpl implements OrganizationProfilePortIn {
    public static final String REQUEST_TYPE = "ORGANIZATION_PROFILE_UPDATE";
    public static final String TARGET_SCHEMA = "iam";
    public static final String TARGET_TABLE = "organizations";

    private static final String ORGANIZATION_PROFILE_READ_OWN = "ORGANIZATION_PROFILE_READ_OWN";
    private static final String ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN = "ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN";
    private static final String ORGANIZATION_PROFILE_REVIEW_ALL = "ORGANIZATION_PROFILE_REVIEW_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final OrganizationProfileApprovalPortOut approvalPortOut;
    private final OrganizationPolicy organizationPolicy;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();

    public OrganizationProfileUseCaseImpl(CurrentAccountPortIn currentAccountPortIn, OrganizationPortOut organizationPortOut,
                                          OrganizationProfileApprovalPortOut approvalPortOut, OrganizationPolicy organizationPolicy) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
        this.approvalPortOut = approvalPortOut;
        this.organizationPolicy = organizationPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationProfileResult getMyProfile() {
        currentAccountPortIn.requirePermission(ORGANIZATION_PROFILE_READ_OWN);
        var currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        Organization organization = resolveCurrentOrganization();
        return new OrganizationProfileResult(
                organization,
                currentAccount.username(),
                currentAccount.email(),
                summarizeOperations(organization.getOrganizationId()),
                approvalPortOut.findLatestByOrganizationId(organization.getOrganizationId())
                        .map(request -> withCurrentProfile(request, organization))
                        .orElse(null)
        );
    }

    @Override
    @Transactional
    public OrganizationProfileUpdateRequestResult requestMyProfileUpdate(RequestOrganizationProfileUpdateCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN);
        if (command == null) throw new BadRequestException("Organization profile update request must not be null");

        Organization approved = resolveCurrentOrganization();
        if (approvalPortOut.existsPendingByOrganizationId(approved.getOrganizationId())) {
            throw new ConflictException("An organization profile update is already pending review");
        }

        // Validate the proposal, but deliberately do not save it as the live organization.
        Organization proposal = copyWithProposedData(approved, command);
        organizationPolicy.initialize(proposal);

        ApprovalRequest request = new ApprovalRequest();
        request.setApprovalRequestId(UUID.randomUUID());
        request.setRequestType(REQUEST_TYPE);
        request.setTargetSchema(TARGET_SCHEMA);
        request.setTargetTable(TARGET_TABLE);
        request.setTargetId(approved.getOrganizationId());
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setRequestedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        request.setRequestData(toRequestData(proposal));
        approvalRequestPolicy.initialize(request);
        approvalPortOut.save(request);
        return withCurrentProfile(toResult(request), approved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationProfileUpdateRequestResult> getProfileUpdateRequests(ApprovalRequestStatus status) {
        currentAccountPortIn.requirePermission(ORGANIZATION_PROFILE_REVIEW_ALL);
        return approvalPortOut.findAll(status).stream()
                .map(request -> organizationPortOut.findById(request.organizationId())
                        .map(organization -> withCurrentProfile(request, organization))
                        .orElse(request))
                .toList();
    }

    @Override
    @Transactional
    public OrganizationProfileUpdateRequestResult approveProfileUpdate(UUID approvalRequestId, ReviewOrganizationProfileUpdateCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_PROFILE_REVIEW_ALL);
        ApprovalRequest request = getPendingRequestForUpdate(approvalRequestId);
        Organization proposed = copyWithRequestData(findOrganization(request.getTargetId()), request.getRequestData());
        organizationPolicy.initialize(proposed);
        Organization approved = organizationPortOut.save(proposed);

        approvalRequestPolicy.approve(request, currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now(), normalizeReviewNote(command));
        approvalPortOut.save(request);
        return withCurrentProfile(toResult(request), approved);
    }

    @Override
    @Transactional
    public OrganizationProfileUpdateRequestResult rejectProfileUpdate(UUID approvalRequestId, ReviewOrganizationProfileUpdateCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_PROFILE_REVIEW_ALL);
        String note = normalizeReviewNote(command);
        if (note == null || note.isBlank()) {
            throw new BadRequestException("Review note is required when rejecting an organization profile update");
        }
        ApprovalRequest request = getPendingRequestForUpdate(approvalRequestId);
        approvalRequestPolicy.reject(request, note);
        approvalPortOut.save(request);
        return organizationPortOut.findById(request.getTargetId())
                .map(organization -> withCurrentProfile(toResult(request), organization))
                .orElseGet(() -> toResult(request));
    }

    private Organization resolveCurrentOrganization() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Set<UUID> organizationIds = organizationPortOut.findActiveOrganizationIdsByAccountId(accountId);
        if (organizationIds.isEmpty()) throw new NotFoundException("Current account does not belong to an active organization");
        if (organizationIds.size() > 1) throw new ConflictException("Current account belongs to multiple organizations; select an organization before updating its profile");
        return findOrganization(organizationIds.iterator().next());
    }

    private ApprovalRequest getPendingRequestForUpdate(UUID approvalRequestId) {
        if (approvalRequestId == null) throw new NotFoundException("Organization profile update request not found");
        ApprovalRequest request = approvalPortOut.findByIdForUpdate(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Organization profile update request not found"));
        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Organization profile update request has already been reviewed");
        }
        return request;
    }

    private Organization findOrganization(UUID organizationId) {
        return organizationPortOut.findById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private Organization copyWithProposedData(Organization approved, RequestOrganizationProfileUpdateCommand command) {
        Organization proposal = baseCopy(approved);
        proposal.setName(command.name());
        proposal.setStructuredAddress(command.structuredAddress());
        return proposal;
    }

    private Organization copyWithRequestData(Organization approved, Map<String, String> data) {
        Organization proposal = baseCopy(approved);
        proposal.setName(requiredValue(data, "organizationName"));
        proposal.setStructuredAddress(new VietnamAddress(requiredValue(data, "provinceCode"), nullableValue(data, "districtCode"),
                requiredValue(data, "wardCode"), requiredValue(data, "addressDetail"), requiredValue(data, "addressDisplay")));
        return proposal;
    }

    private Organization baseCopy(Organization source) {
        Organization copy = new Organization();
        copy.setOrganizationId(source.getOrganizationId());
        copy.setCode(source.getCode());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setUpdatedBy(source.getUpdatedBy());
        return copy;
    }

    private Map<String, String> toRequestData(Organization organization) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("organizationCode", organization.getCode());
        data.put("organizationName", organization.getName());
        data.put("addressDetail", organization.getAddressDetail());
        data.put("provinceCode", organization.getProvinceCode());
        data.put("wardCode", organization.getWardCode());
        putOrRemove(data, "districtCode", organization.getDistrictCode());
        data.put("addressDisplay", organization.getAddressDisplay());
        return Map.copyOf(data);
    }

    private OrganizationProfileUpdateRequestResult toResult(ApprovalRequest request) {
        Map<String, String> data = request.getRequestData();
        return new OrganizationProfileUpdateRequestResult(request.getApprovalRequestId(), request.getTargetId(),
                requiredValue(data, "organizationCode"), requiredValue(data, "organizationName"), requiredValue(data, "addressDetail"),
                requiredValue(data, "provinceCode"), nullableValue(data, "districtCode"), requiredValue(data, "wardCode"),
                requiredValue(data, "addressDisplay"), request.getStatus(), request.getCreatedAt(), request.getNote(), null, null);
    }

    private OrganizationProfileOperationalSummary summarizeOperations(UUID organizationId) {
        return new OrganizationProfileOperationalSummary(
                organizationPortOut.countParkingLotsByOrganizationId(organizationId),
                organizationPortOut.countParkingLotsByOrganizationIdAndStatus(organizationId, ParkingLotStatus.ACTIVE),
                organizationPortOut.countParkingLotsByOrganizationIdAndStatus(organizationId, ParkingLotStatus.SETUP),
                organizationPortOut.countParkingLotsByOrganizationIdAndStatus(organizationId, ParkingLotStatus.MAINTENANCE)
        );
    }

    /** Adds the immutable, currently effective values so reviewers can compare before approving. */
    private OrganizationProfileUpdateRequestResult withCurrentProfile(
            OrganizationProfileUpdateRequestResult request,
            Organization organization
    ) {
        return new OrganizationProfileUpdateRequestResult(
                request.approvalRequestId(), request.organizationId(), request.organizationCode(), request.organizationName(),
                request.addressDetail(), request.provinceCode(), request.districtCode(), request.wardCode(), request.addressDisplay(),
                request.status(), request.submittedAt(), request.note(), organization.getName(), organization.getAddressDisplay()
        );
    }

    private String normalizeReviewNote(ReviewOrganizationProfileUpdateCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private String requiredValue(Map<String, String> data, String key) {
        String value = data == null ? null : data.get(key);
        if (value == null || value.isBlank()) throw new BadRequestException("Organization profile update request is incomplete");
        return value;
    }

    private String nullableValue(Map<String, String> data, String key) {
        String value = data == null ? null : data.get(key);
        return value == null || value.isBlank() ? null : value;
    }

    private void putOrRemove(Map<String, String> data, String key, String value) {
        if (value == null || value.isBlank()) data.remove(key); else data.put(key, value);
    }
}
