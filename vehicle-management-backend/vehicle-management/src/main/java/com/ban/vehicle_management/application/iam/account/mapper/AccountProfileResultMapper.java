package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AccountProfileResultMapper {

    @Mapping(target = "account", source = "state")
    @Mapping(target = "profile", source = "state")
    @Mapping(target = "employee", expression = "java(toEmployeeInfoResult(state))")
    @Mapping(target = "customer", expression = "java(toCustomerInfoResult(state))")
    AccountProfileStatusResult toStatusResult(AccountProfileState state, boolean onboardingRequired);

    @Mapping(target = "onboardingRequired", constant = "false")
    @Mapping(target = "account", source = "updatedAccount")
    @Mapping(target = "profile", expression = "java(toProfileInfoResult(userProfile, userProfileId))")
    @Mapping(target = "employee", expression = "java(toEmployeeInfoResult(employee, employeeId))")
    @Mapping(target = "customer", expression = "java(toCustomerInfoResult(customer, customerId))")
    AccountProfileStatusResult toStatusResult(
            Account updatedAccount,
            UserProfile userProfile,
            Employee employee,
            Customer customer,
            UUID userProfileId,
            UUID employeeId,
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

    default UserProfileStatus resolveUserProfileStatus(UserProfileStatus userProfileStatus) {
        return userProfileStatus != null ? userProfileStatus : UserProfileStatus.ACTIVE;
    }

    default AccountProfileStatusResult.EmployeeInfoResult toEmployeeInfoResult(AccountProfileState state) {
        if (state == null || !shouldExposeEmployee(state)) {
            return null;
        }
        return new AccountProfileStatusResult.EmployeeInfoResult(
                state.employeeId(),
                state.employeeCode(),
                state.jobTitle(),
                state.employeeHiredAt(),
                enumName(state.employeeStatus())
        );
    }

    default AccountProfileStatusResult.CustomerInfoResult toCustomerInfoResult(AccountProfileState state) {
        if (state == null || !shouldExposeCustomer(state)) {
            return null;
        }
        return new AccountProfileStatusResult.CustomerInfoResult(
                state.customerId(),
                state.customerCode(),
                enumName(state.customerType()),
                enumName(state.customerStatus()),
                enumName(state.customerApprovalStatus())
        );
    }

    default AccountProfileStatusResult.EmployeeInfoResult toEmployeeInfoResult(Employee employee, UUID employeeId) {
        if (employee == null && employeeId == null) {
            return null;
        }
        return new AccountProfileStatusResult.EmployeeInfoResult(
                employeeId,
                employee == null ? null : employee.getEmployeeCode(),
                employee == null ? null : employee.getJobTitle(),
                employee == null ? null : employee.getHiredAt(),
                employee == null ? null : enumName(employee.getStatus())
        );
    }

    default AccountProfileStatusResult.CustomerInfoResult toCustomerInfoResult(Customer customer, UUID customerId) {
        if (customer == null && customerId == null) {
            return null;
        }
        return new AccountProfileStatusResult.CustomerInfoResult(
                customerId,
                customer == null ? null : customer.getCustomerCode(),
                customer == null ? null : enumName(customer.getCustomerType()),
                customer == null ? null : enumName(customer.getStatus()),
                customer == null ? null : enumName(customer.getApprovalStatus())
        );
    }

    default boolean shouldExposeEmployee(AccountProfileState state) {
        if (hasEmployeeData(state)) {
            return true;
        }
        AdminProvisionableAccountRoleCode roleCode = resolveRole(state.roleCode());
        return AdminProvisionableAccountRoleCode.EMPLOYEE.equals(roleCode)
                || AdminProvisionableAccountRoleCode.PARKING_MANAGER.equals(roleCode);
    }

    default boolean shouldExposeCustomer(AccountProfileState state) {
        if (hasCustomerData(state)) {
            return true;
        }
        return AdminProvisionableAccountRoleCode.CUSTOMER.equals(resolveRole(state.roleCode()));
    }

    default boolean hasEmployeeData(AccountProfileState state) {
        return state.employeeId() != null
                || state.employeeCode() != null
                || state.jobTitle() != null
                || state.employeeHiredAt() != null
                || state.employeeStatus() != null;
    }

    default boolean hasCustomerData(AccountProfileState state) {
        return state.customerId() != null
                || state.customerCode() != null
                || state.customerType() != null
                || state.customerStatus() != null
                || state.customerApprovalStatus() != null;
    }

    default AdminProvisionableAccountRoleCode resolveRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
