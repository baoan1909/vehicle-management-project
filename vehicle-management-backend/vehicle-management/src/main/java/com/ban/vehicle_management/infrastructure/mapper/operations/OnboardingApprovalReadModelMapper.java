package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OnboardingApprovalReadModelMapper {

    @Mapping(target = "request", source = "approvalRequestEntity")
    @Mapping(target = "account", expression = "java(toCustomerAccountInfo(accountEntity, roleEntity))")
    @Mapping(target = "profile", source = "userProfileEntity")
    @Mapping(target = "customer", source = "customerEntity")
    CustomerOnboardingApprovalResult toCustomerResult(
            ApprovalRequestEntity approvalRequestEntity,
            AccountEntity accountEntity,
            RoleEntity roleEntity,
            UserProfileEntity userProfileEntity,
            CustomerEntity customerEntity
    );

    @Mapping(target = "request", source = "approvalRequestEntity")
    @Mapping(target = "account", expression = "java(toInternalEmployeeAccountInfo(accountEntity, roleEntity))")
    @Mapping(target = "profile", source = "userProfileEntity")
    @Mapping(target = "employee", source = "employeeEntity")
    InternalEmployeeApprovalResult toInternalEmployeeResult(
            ApprovalRequestEntity approvalRequestEntity,
            AccountEntity accountEntity,
            RoleEntity roleEntity,
            UserProfileEntity userProfileEntity,
            EmployeeEntity employeeEntity
    );

    @Mapping(target = "request", source = "approvalRequestEntity")
    @Mapping(target = "account", expression = "java(toSystemAdminAccountInfo(accountEntity, roleEntity))")
    @Mapping(target = "profile", source = "userProfileEntity")
    SystemAdminApprovalResult toSystemAdminResult(
            ApprovalRequestEntity approvalRequestEntity,
            AccountEntity accountEntity,
            RoleEntity roleEntity,
            UserProfileEntity userProfileEntity
    );

    @Mapping(target = "approvalRequestStatus", source = "status")
    CustomerOnboardingApprovalResult.RequestInfoResult toCustomerRequestInfo(ApprovalRequestEntity entity);

    @Mapping(target = "approvalRequestStatus", source = "status")
    InternalEmployeeApprovalResult.RequestInfoResult toInternalEmployeeRequestInfo(ApprovalRequestEntity entity);

    @Mapping(target = "approvalRequestStatus", source = "status")
    SystemAdminApprovalResult.RequestInfoResult toSystemAdminRequestInfo(ApprovalRequestEntity entity);

    @Mapping(target = "roleCode", source = "roleEntity.code")
    @Mapping(target = "accountStatus", source = "accountEntity.status")
    CustomerOnboardingApprovalResult.AccountInfoResult toCustomerAccountInfo(AccountEntity accountEntity, RoleEntity roleEntity);

    @Mapping(target = "roleCode", source = "roleEntity.code")
    @Mapping(target = "accountStatus", source = "accountEntity.status")
    InternalEmployeeApprovalResult.AccountInfoResult toInternalEmployeeAccountInfo(AccountEntity accountEntity, RoleEntity roleEntity);

    @Mapping(target = "roleCode", source = "roleEntity.code")
    @Mapping(target = "accountStatus", source = "accountEntity.status")
    SystemAdminApprovalResult.AccountInfoResult toSystemAdminAccountInfo(AccountEntity accountEntity, RoleEntity roleEntity);

    CustomerOnboardingApprovalResult.ProfileInfoResult toCustomerProfileInfo(UserProfileEntity entity);

    InternalEmployeeApprovalResult.ProfileInfoResult toInternalEmployeeProfileInfo(UserProfileEntity entity);

    SystemAdminApprovalResult.ProfileInfoResult toSystemAdminProfileInfo(UserProfileEntity entity);

    @Mapping(target = "customerStatus", source = "status")
    @Mapping(target = "customerApprovalStatus", source = "approvalStatus")
    CustomerOnboardingApprovalResult.CustomerInfoResult toCustomerInfo(CustomerEntity entity);

    @Mapping(target = "employeeStatus", source = "status")
    InternalEmployeeApprovalResult.EmployeeInfoResult toEmployeeInfo(EmployeeEntity entity);
}
