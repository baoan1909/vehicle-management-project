package com.ban.vehicle_management.entrypoint.dto.people.customer.response;

import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerAdminResponse {

    private UUID customerId;
    private UUID userProfileId;
    private String customerCode;
    private CustomerType customerType;
    private CustomerStatus status;
    private CustomerApprovalStatus approvalStatus;
    private UUID approvedBy;
    private String approvedAt;
    private String accountEmail;
    private UserProfileAdminResponse userProfile;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}

