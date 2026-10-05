package com.ban.vehicle_management.domain.iam.partnerregistration.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartnerApprovalValidatorTest {

    private final PartnerApprovalValidator validator = new PartnerApprovalValidator();

    @Test
    void shouldAcceptCurrentActiveAvatarAndCompleteProfile() {
        Fixture fixture = completeFixture();

        assertDoesNotThrow(() -> validator.validateBeforeApproval(
                fixture.approval(),
                fixture.account(),
                fixture.profile(),
                List.of(fixture.avatar()),
                "PARTNER_ABC",
                "Đối tác ABC",
                "Nguyễn Văn A",
                "0901234567",
                fixture.organizationAddress()
        ));
    }

    @Test
    void shouldAcceptCompleteProfileWithoutApprovedAvatar() {
        Fixture fixture = completeFixture();
        fixture.avatar().setStatus(UserProfileAvatarStatus.REPLACED);

        assertDoesNotThrow(() -> validator.validateBeforeApproval(
                fixture.approval(),
                fixture.account(),
                fixture.profile(),
                List.of(fixture.avatar()),
                "PARTNER_ABC",
                "Đối tác ABC",
                "Nguyễn Văn A",
                "0901234567",
                fixture.organizationAddress()
        ));
    }

    private Fixture completeFixture() {
        UUID accountId = UUID.randomUUID();
        ApprovalRequest approval = new ApprovalRequest();
        approval.setRequestedBy(accountId);
        approval.setStatus(ApprovalRequestStatus.PENDING);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setStatus(AccountStatus.PENDING);

        UserProfile profile = new UserProfile();
        profile.setFullName("Nguyễn Văn A");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setGender("Nam");
        profile.setPhoneNumber("0901234567");
        profile.setIdentifyCard("079090000001");
        profile.setStructuredAddress(VietnamAddress.ofCurrent("79", "26734", "12 Nguyễn Văn A"));

        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);

        Organization.OrganizationAddress organizationAddress = new Organization.OrganizationAddress();
        organizationAddress.setStructuredAddress(
                VietnamAddress.ofLegacy("79", "760", "26734", "12 Nguyễn Văn B")
        );
        return new Fixture(approval, account, profile, avatar, organizationAddress);
    }

    private record Fixture(
            ApprovalRequest approval,
            Account account,
            UserProfile profile,
            UserProfileAvatar avatar,
            Organization.OrganizationAddress organizationAddress
    ) {
    }
}
