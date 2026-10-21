package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationProfileUpdateRequestResponse {
    private UUID approvalRequestId;
    private UUID organizationId;
    private String organizationCode;
    private String organizationName;
    private String addressDetail;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String addressDisplay;
    private ApprovalRequestStatus status;
    private Instant submittedAt;
    private String note;
    private String currentOrganizationName;
    private String currentAddressDisplay;
}
