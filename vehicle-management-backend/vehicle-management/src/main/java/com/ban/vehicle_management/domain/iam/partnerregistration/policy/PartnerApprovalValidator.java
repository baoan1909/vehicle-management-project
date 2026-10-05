package com.ban.vehicle_management.domain.iam.partnerregistration.policy;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PartnerApprovalValidator {

    public void validateBeforeApproval(
            ApprovalRequest approvalRequest,
            Account applicantAccount,
            UserProfile userProfile,
            List<UserProfileAvatar> avatars,
            String organizationCode,
            String organizationName,
            String representativeName,
            String representativePhoneNumber,
            Organization.OrganizationAddress organizationAddress
    ) {
        // 1. ApprovalRequest must be PENDING
        if (approvalRequest.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Partner registration has already been reviewed");
        }

        // 2. Account must be PENDING
        if (applicantAccount.getStatus() != AccountStatus.PENDING) {
            throw new ConflictException("Partner applicant account is not pending");
        }

        // 3. Account must be the applicant
        if (!applicantAccount.getAccountId().equals(approvalRequest.getRequestedBy())) {
            throw new ConflictException("Account does not match approval request applicant");
        }

        // 4. Account must have PARTNER_ADMIN role (roleId check is done in use case)

        // 5. Email must be verified (checked externally via Keycloak)

        // 6. User profile must exist
        if (userProfile == null) {
            throw new BadRequestException("User profile not found");
        }

        // 7. Full name must be valid
        if (userProfile.getFullName() == null || userProfile.getFullName().isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        // 8. Phone number must exist and be unique (uniqueness is checked by the use case)
        if (userProfile.getPhoneNumber() == null || userProfile.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Phone number is required");
        }

        // 9. Date of birth must be valid
        if (userProfile.getDateOfBirth() == null) {
            throw new BadRequestException("Date of birth is required");
        }

        // 10. Gender must be valid
        if (userProfile.getGender() == null || userProfile.getGender().isBlank()) {
            throw new BadRequestException("Gender is required");
        }

        // 11. Identify card must exist and be unique (checked at profile level)
        if (userProfile.getIdentifyCard() == null || userProfile.getIdentifyCard().isBlank()) {
            throw new BadRequestException("Identify card is required");
        }

        // Avatar moderation is independent from onboarding and account activation.

        // 13. Personal address must be valid
        if (userProfile.getStructuredAddress() == null) {
            throw new BadRequestException("Personal address is required");
        }
        // Address validation is done by VietnamAddressPolicy at profile update

        // 14. Organization data must be valid
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new BadRequestException("Organization code is required");
        }
        if (organizationName == null || organizationName.isBlank()) {
            throw new BadRequestException("Organization name is required");
        }
        if (representativeName == null || representativeName.isBlank()) {
            throw new BadRequestException("Representative name is required");
        }
        if (representativePhoneNumber == null || representativePhoneNumber.isBlank()) {
            throw new BadRequestException("Representative phone number is required");
        }

        // 15. Organization address must be valid
        if (organizationAddress == null || organizationAddress.getStructuredAddress() == null) {
            throw new BadRequestException("Organization address is required");
        }
        // Address validation is done by VietnamAddressPolicy

        // 16. Organization code must not exist
        // This is checked in the use case

        // 17. No existing organization or membership from previous approval
        // This is checked in the use case
    }
}
