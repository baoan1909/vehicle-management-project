package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountProfileResultMapper {

    @Mapping(target = "account", source = "state")
    @Mapping(target = "profile", source = "state")
    @Mapping(target = "customer", source = "state")
    AccountProfileStatusResult toStatusResult(AccountProfileState state, boolean onboardingRequired);

    @Mapping(target = "onboardingRequired", constant = "false")
    @Mapping(target = "account", source = "updatedAccount")
    @Mapping(target = "profile", expression = "java(toProfileInfoResult(userProfile, userProfileId))")
    @Mapping(target = "customer", expression = "java(toCustomerInfoResult(customer, customerId))")
    AccountProfileStatusResult toStatusResult(
            Account updatedAccount,
            UserProfile userProfile,
            Customer customer,
            UUID userProfileId,
            UUID customerId
    );

    @Mapping(target = "userProfileId", source = "state.userProfileId")
    @Mapping(target = "fullName", expression = "java(firstNonNull(command.fullName(), state.fullName()))")
    @Mapping(target = "phoneNumber", expression = "java(firstNonNull(command.phoneNumber(), state.phoneNumber()))")
    @Mapping(target = "dateOfBirth", expression = "java(command.dateOfBirth() != null ? command.dateOfBirth() : state.dateOfBirth())")
    @Mapping(target = "gender", expression = "java(firstNonNull(command.gender(), state.gender()))")
    @Mapping(target = "address", expression = "java(firstNonNull(command.address(), state.address()))")
    @Mapping(target = "identifyCard", expression = "java(firstNonNull(command.identifyCard(), state.identifyCard()))")
    @Mapping(target = "avatarUrl", expression = "java(firstNonNull(command.avatarUrl(), state.avatarUrl()))")
    @Mapping(target = "status", expression = "java(resolveUserProfileStatus(state.userProfileStatus()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserProfile mergeProfile(AccountProfileState state, UpdateAccountProfileCommand command);

    default <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    @Mapping(target = "accountStatus", source = "accountStatus")
    AccountProfileStatusResult.AccountInfoResult toAccountInfoResult(AccountProfileState state);

    @Mapping(target = "userProfileStatus", source = "userProfileStatus")
    AccountProfileStatusResult.ProfileInfoResult toProfileInfoResult(AccountProfileState state);

    @Mapping(target = "customerType", source = "customerType")
    @Mapping(target = "customerStatus", source = "customerStatus")
    @Mapping(target = "customerApprovalStatus", source = "customerApprovalStatus")
    AccountProfileStatusResult.CustomerInfoResult toCustomerInfoResult(AccountProfileState state);

    @Mapping(target = "accountStatus", source = "status")
    AccountProfileStatusResult.AccountInfoResult toAccountInfoResult(Account account);

    @Mapping(target = "userProfileId", source = "userProfileId")
    @Mapping(target = "fullName", source = "userProfile.fullName")
    @Mapping(target = "dateOfBirth", source = "userProfile.dateOfBirth")
    @Mapping(target = "gender", source = "userProfile.gender")
    @Mapping(target = "phoneNumber", source = "userProfile.phoneNumber")
    @Mapping(target = "address", source = "userProfile.address")
    @Mapping(target = "identifyCard", source = "userProfile.identifyCard")
    @Mapping(target = "avatarUrl", source = "userProfile.avatarUrl")
    @Mapping(target = "userProfileStatus", source = "userProfile.status")
    AccountProfileStatusResult.ProfileInfoResult toProfileInfoResult(UserProfile userProfile, UUID userProfileId);

    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "customerCode", source = "customer.customerCode")
    @Mapping(target = "customerType", source = "customer.customerType")
    @Mapping(target = "customerStatus", source = "customer.status")
    @Mapping(target = "customerApprovalStatus", source = "customer.approvalStatus")
    AccountProfileStatusResult.CustomerInfoResult toCustomerInfoResult(Customer customer, UUID customerId);

    default UserProfileStatus resolveUserProfileStatus(UserProfileStatus userProfileStatus) {
        return userProfileStatus != null ? userProfileStatus : UserProfileStatus.ACTIVE;
    }
}
