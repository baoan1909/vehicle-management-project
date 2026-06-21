package com.ban.vehicle_management.domain.people.customer.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends AuditableDomainModel {

    private UUID customerId;
    private UUID userProfileId;
    private String customerCode;
    private CustomerType customerType;
    private CustomerStatus status;
    private CustomerApprovalStatus approvalStatus;
    private UUID approvedBy;
    private Instant approvedAt;
    private String accountEmail;
    private UserProfile userProfile;
}

