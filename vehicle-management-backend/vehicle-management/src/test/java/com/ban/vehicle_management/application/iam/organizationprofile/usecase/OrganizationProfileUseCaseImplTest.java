package com.ban.vehicle_management.application.iam.organizationprofile.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.RequestOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.ReviewOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.application.iam.organizationprofile.port.out.OrganizationProfileApprovalPortOut;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies that partner changes remain pending until the platform approves them. */
@ExtendWith(MockitoExtension.class)
class OrganizationProfileUseCaseImplTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private OrganizationProfileApprovalPortOut approvalPortOut;
    @Mock private OrganizationPolicy organizationPolicy;

    private OrganizationProfileUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new OrganizationProfileUseCaseImpl(
                currentAccountPortIn, organizationPortOut, approvalPortOut, organizationPolicy
        );
    }

    @Test
    void shouldSaveOnlyPendingApprovalWhenPartnerRequestsProfileChange() {
        UUID accountId = UUID.randomUUID();
        Organization organization = organization();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId))
                .thenReturn(Set.of(organization.getOrganizationId()));
        when(organizationPortOut.findById(organization.getOrganizationId())).thenReturn(Optional.of(organization));
        when(approvalPortOut.existsPendingByOrganizationId(organization.getOrganizationId())).thenReturn(false);

        useCase.requestMyProfileUpdate(new RequestOrganizationProfileUpdateCommand(
                "Bãi xe Đối tác đã đổi tên", proposedAddress()
        ));

        ArgumentCaptor<ApprovalRequest> requestCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(currentAccountPortIn).requirePermission("ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN");
        verify(approvalPortOut).save(requestCaptor.capture());
        verify(organizationPortOut, never()).save(any());

        ApprovalRequest request = requestCaptor.getValue();
        assertEquals(ApprovalRequestStatus.PENDING, request.getStatus());
        assertEquals(organization.getOrganizationId(), request.getTargetId());
        assertEquals("Bãi xe Đối tác đã đổi tên", request.getRequestData().get("organizationName"));
        assertEquals("Số 1 đường Võ Văn Ngân, Phường Bình Thọ, Thành phố Hồ Chí Minh",
                request.getRequestData().get("addressDisplay"));
    }

    @Test
    void shouldPromoteProposedDataOnlyWhenSystemAdminApproves() {
        UUID reviewerId = UUID.randomUUID();
        Organization organization = organization();
        ApprovalRequest request = pendingRequest(organization.getOrganizationId());
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(reviewerId);
        when(approvalPortOut.findByIdForUpdate(request.getApprovalRequestId())).thenReturn(Optional.of(request));
        when(organizationPortOut.findById(organization.getOrganizationId())).thenReturn(Optional.of(organization));
        when(organizationPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.approveProfileUpdate(request.getApprovalRequestId(), new ReviewOrganizationProfileUpdateCommand("Đủ hồ sơ"));

        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(currentAccountPortIn).requirePermission("ORGANIZATION_PROFILE_REVIEW_ALL");
        verify(organizationPortOut).save(organizationCaptor.capture());
        verify(approvalPortOut).save(request);
        assertEquals("Bãi xe Đối tác đã đổi tên", organizationCaptor.getValue().getName());
        assertEquals(ApprovalRequestStatus.APPROVED, request.getStatus());
        assertEquals(reviewerId, request.getApprovedBy());
    }

    @Test
    void shouldRequireReasonWhenSystemAdminRejectsProfileChange() {
        assertThrows(BadRequestException.class,
                () -> useCase.rejectProfileUpdate(UUID.randomUUID(), new ReviewOrganizationProfileUpdateCommand(" ")));

        verify(currentAccountPortIn).requirePermission("ORGANIZATION_PROFILE_REVIEW_ALL");
        verify(approvalPortOut, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldExposeCurrentProfileValuesWhenSystemAdminListsUpdateRequests() {
        Organization organization = organization();
        OrganizationProfileUpdateRequestResult request = new OrganizationProfileUpdateRequestResult(
                UUID.randomUUID(), organization.getOrganizationId(), organization.getCode(), "Tên đề xuất",
                "Số 99", "79", null, "26734", "Số 99, Phường Bình Thọ, Thành phố Hồ Chí Minh",
                ApprovalRequestStatus.PENDING, java.time.Instant.now(), null, null, null
        );
        when(approvalPortOut.findAll(ApprovalRequestStatus.PENDING)).thenReturn(List.of(request));
        when(organizationPortOut.findById(organization.getOrganizationId())).thenReturn(Optional.of(organization));

        OrganizationProfileUpdateRequestResult result = useCase
                .getProfileUpdateRequests(ApprovalRequestStatus.PENDING)
                .getFirst();

        verify(currentAccountPortIn).requirePermission("ORGANIZATION_PROFILE_REVIEW_ALL");
        assertEquals("Bãi xe Đối tác", result.currentOrganizationName());
        assertEquals("Số 1 đường Võ Văn Ngân, Phường Bình Thọ, Thành phố Hồ Chí Minh", result.currentAddressDisplay());
        assertEquals("Tên đề xuất", result.organizationName());
    }

    private Organization organization() {
        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setCode("PARTNER_ABC");
        organization.setName("Bãi xe Đối tác");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setStructuredAddress(new VietnamAddress(
                "79", null, "26734", "Số 1 đường Võ Văn Ngân",
                "Số 1 đường Võ Văn Ngân, Phường Bình Thọ, Thành phố Hồ Chí Minh"
        ));
        return organization;
    }

    private VietnamAddress proposedAddress() {
        return new VietnamAddress(
                "79", null, "26734", "Số 1 đường Võ Văn Ngân",
                "Số 1 đường Võ Văn Ngân, Phường Bình Thọ, Thành phố Hồ Chí Minh"
        );
    }

    private ApprovalRequest pendingRequest(UUID organizationId) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApprovalRequestId(UUID.randomUUID());
        request.setRequestType(OrganizationProfileUseCaseImpl.REQUEST_TYPE);
        request.setTargetSchema(OrganizationProfileUseCaseImpl.TARGET_SCHEMA);
        request.setTargetTable(OrganizationProfileUseCaseImpl.TARGET_TABLE);
        request.setTargetId(organizationId);
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setRequestData(Map.of(
                "organizationCode", "PARTNER_ABC",
                "organizationName", "Bãi xe Đối tác đã đổi tên",
                "provinceCode", "79",
                "wardCode", "26734",
                "addressDetail", "Số 1 đường Võ Văn Ngân",
                "addressDisplay", "Số 1 đường Võ Văn Ngân, Phường Bình Thọ, Thành phố Hồ Chí Minh"
        ));
        return request;
    }
}
