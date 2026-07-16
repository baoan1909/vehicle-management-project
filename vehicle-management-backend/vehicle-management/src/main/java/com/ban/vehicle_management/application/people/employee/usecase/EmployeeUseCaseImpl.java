package com.ban.vehicle_management.application.people.employee.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.model.command.UpdateEmployeeAdminProfileCommand;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeeUseCaseImpl implements EmployeePortIn {

    private static final String EMPLOYEE_READ_ALL = "EMPLOYEE_READ_ALL";
    private static final String EMPLOYEE_UPDATE_ALL = "EMPLOYEE_UPDATE_ALL";
    private static final String EMPLOYEE_DELETE_ALL = "EMPLOYEE_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final EmployeeAccessGuard employeeAccessGuard;
    private final EmployeePortOut employeePortOut;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final UserProfileAvatarPortIn userProfileAvatarPortIn;
    private final UserProfilePortOut userProfilePortOut;
    private final EmployeePolicy employeePolicy = new EmployeePolicy();
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public EmployeeUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeeAccessGuard employeeAccessGuard,
            EmployeePortOut employeePortOut,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            UserProfileAvatarPortIn userProfileAvatarPortIn,
            UserProfilePortOut userProfilePortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.employeeAccessGuard = employeeAccessGuard;
        this.employeePortOut = employeePortOut;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.userProfileAvatarPortIn = userProfileAvatarPortIn;
        this.userProfilePortOut = userProfilePortOut;
    }

    @Override
    @Transactional
    public Employee updateEmployee(UUID employeeId, Employee employee) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee existingEmployee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(existingEmployee);

        existingEmployee.setEmployeeCode(employee.getEmployeeCode());
        existingEmployee.setJobTitle(employee.getJobTitle());
        existingEmployee.setHiredAt(employee.getHiredAt());
        if (employee.getStatus() != null && !Objects.equals(employee.getStatus(), existingEmployee.getStatus())) {
            throw new BadRequestException("Use activate, inactivate, or suspend endpoints to change employee status");
        }

        employeePolicy.validateState(existingEmployee);

        if (employeePortOut.existsByEmployeeCodeAndEmployeeIdNot(existingEmployee.getEmployeeCode(), employeeId)) {
            throw new ConflictException("Employee code already exists");
        }

        return saveEmployeeAndReload(existingEmployee);
    }

    @Override
    @Transactional
    public Employee updateEmployeeAdminProfile(UUID employeeId, UpdateEmployeeAdminProfileCommand command) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        ensureAdminProfilePayloadHasContent(command);

        Employee existingEmployee = getEmployeeById(employeeId);
        ensureEmployeeUserProfileExists(existingEmployee);
        employeeAccessGuard.ensureCanManage(existingEmployee);

        if (hasEmployeeChanges(command.employee())) {
            applyEmployeeChanges(existingEmployee, command.employee());
            employeePolicy.validateState(existingEmployee);

            if (employeePortOut.existsByEmployeeCodeAndEmployeeIdNot(existingEmployee.getEmployeeCode(), employeeId)) {
                throw new ConflictException("Employee code already exists");
            }

            existingEmployee = employeePortOut.save(existingEmployee);
        }

        if (hasUserProfileChanges(command.userProfile())) {
            UserProfile existingUserProfile = userProfilePortOut.findById(existingEmployee.getUserProfileId())
                    .orElseThrow(() -> new ConflictException("Employee user profile is missing"));
            applyUserProfileChanges(existingUserProfile, command.userProfile());
            userProfilePolicy.validateState(existingUserProfile);
            validateUniqueUserProfile(existingUserProfile, existingUserProfile.getUserProfileId());
            UserProfile updatedUserProfile = userProfilePortOut.save(existingUserProfile);
            existingEmployee.setUserProfile(updatedUserProfile);
        }

        return reloadEmployeeForResponse(employeeId);
    }

    private void ensureAdminProfilePayloadHasContent(UpdateEmployeeAdminProfileCommand command) {
        if (command == null || (!hasEmployeeChanges(command.employee())
                && !hasUserProfileChanges(command.userProfile()))) {
            throw new BadRequestException("At least one profile field or employee field must be provided");
        }
    }

    private void ensureEmployeeUserProfileExists(Employee employee) {
        if (employee == null || employee.getUserProfileId() == null || employee.getUserProfile() == null) {
            throw new ConflictException("Employee user profile is missing");
        }
    }

    private void applyEmployeeChanges(Employee existingEmployee, Employee updatedEmployee) {
        existingEmployee.setEmployeeCode(updatedEmployee.getEmployeeCode());
        existingEmployee.setJobTitle(updatedEmployee.getJobTitle());
        existingEmployee.setHiredAt(updatedEmployee.getHiredAt());
        if (updatedEmployee.getStatus() != null && !Objects.equals(updatedEmployee.getStatus(), existingEmployee.getStatus())) {
            throw new BadRequestException("Use activate, inactivate, or suspend endpoints to change employee status");
        }
    }

    private void applyUserProfileChanges(UserProfile existingUserProfile, UserProfile updatedUserProfile) {
        existingUserProfile.setFullName(updatedUserProfile.getFullName());
        existingUserProfile.setDateOfBirth(updatedUserProfile.getDateOfBirth());
        existingUserProfile.setGender(updatedUserProfile.getGender());
        existingUserProfile.setPhoneNumber(updatedUserProfile.getPhoneNumber());
        existingUserProfile.setAddress(updatedUserProfile.getAddress());
        existingUserProfile.setIdentifyCard(updatedUserProfile.getIdentifyCard());
        if (updatedUserProfile.getStatus() != null) {
            existingUserProfile.setStatus(updatedUserProfile.getStatus());
        }
    }

    private boolean hasEmployeeChanges(Employee employee) {
        return employee != null
                && (employee.getEmployeeCode() != null
                || employee.getJobTitle() != null
                || employee.getHiredAt() != null
                || employee.getStatus() != null);
    }

    private boolean hasUserProfileChanges(UserProfile userProfile) {
        return userProfile != null
                && (userProfile.getFullName() != null
                || userProfile.getDateOfBirth() != null
                || userProfile.getGender() != null
                || userProfile.getPhoneNumber() != null
                || userProfile.getAddress() != null
                || userProfile.getIdentifyCard() != null
                || userProfile.getStatus() != null);
    }

    private void validateUniqueUserProfile(UserProfile userProfile, UUID userProfileId) {
        if (userProfile.getPhoneNumber() != null
                && userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(userProfile.getPhoneNumber(), userProfileId)) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null
                && userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(userProfile.getIdentifyCard(), userProfileId)) {
            throw new ConflictException("User profile identify card already exists");
        }
    }

    @Override
    @Transactional
    public Employee uploadEmployeeAvatar(UUID employeeId, MultipartFile file) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        UUID uploaderAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);

        UserProfile updatedUserProfile = userProfileAvatarPortIn.uploadAvatar(
                employee.getUserProfileId(),
                file,
                uploaderAccountId
        );
        employee.setUserProfile(updatedUserProfile);
        return withResolvedAvatarUrl(employee);
    }

    @Override
    @Transactional
    public Employee deleteEmployeeAvatar(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);

        UserProfile updatedUserProfile = userProfileAvatarPortIn.deleteAvatar(employee.getUserProfileId());
        employee.setUserProfile(updatedUserProfile);
        return withResolvedAvatarUrl(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeById(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_READ_ALL);
        Employee employee = employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        employeeAccessGuard.ensureCanRead(employee);
        return withResolvedAvatarUrl(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getEmployees(EmployeeStatus status, String keyword) {
        currentAccountPortIn.requirePermission(EMPLOYEE_READ_ALL);
        List<Employee> employees = employeeAccessGuard.filterReadableEmployees(employeePortOut.findAll(status, keyword));
        return withResolvedAvatarUrls(employees);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_DELETE_ALL);
        Employee existingEmployee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(existingEmployee);
        if (existingEmployee.getStatus() == EmployeeStatus.INACTIVE) {
            return;
        }

        employeePolicy.inactivate(existingEmployee);
        employeePortOut.save(existingEmployee);
    }

    @Override
    @Transactional
    public Employee activateEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);
        ensureInternalOnboardingApprovalSatisfied(employeeId);
        employeePolicy.activate(employee);
        return saveEmployeeAndReload(employee);
    }

    @Override
    @Transactional
    public Employee inactivateEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);
        employeePolicy.inactivate(employee);
        return saveEmployeeAndReload(employee);
    }

    @Override
    @Transactional
    public Employee suspendEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);
        employeePolicy.suspend(employee);
        return saveEmployeeAndReload(employee);
    }

    private Employee saveEmployeeAndReload(Employee employee) {
        Employee savedEmployee = employeePortOut.save(employee);
        return reloadEmployeeForResponse(savedEmployee.getEmployeeId());
    }

    private Employee reloadEmployeeForResponse(UUID employeeId) {
        return withResolvedAvatarUrl(employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found")));
    }

    private Employee withResolvedAvatarUrl(Employee employee) {
        if (employee == null || employee.getUserProfile() == null) {
            return employee;
        }
        employee.setUserProfile(userProfileAvatarPortIn.withResolvedAvatarUrl(employee.getUserProfile()));
        return employee;
    }

    private List<Employee> withResolvedAvatarUrls(List<Employee> employees) {
        List<UserProfile> userProfiles = employees.stream()
                .map(Employee::getUserProfile)
                .filter(Objects::nonNull)
                .toList();
        if (userProfiles.isEmpty()) {
            return employees;
        }

        Map<UUID, UserProfile> resolvedProfilesById = userProfileAvatarPortIn.withResolvedAvatarUrls(userProfiles)
                .stream()
                .filter(Objects::nonNull)
                .filter(userProfile -> userProfile.getUserProfileId() != null)
                .collect(Collectors.toMap(UserProfile::getUserProfileId, userProfile -> userProfile));

        employees.forEach(employee -> {
            if (employee.getUserProfile() == null || employee.getUserProfile().getUserProfileId() == null) {
                return;
            }
            UserProfile resolvedProfile = resolvedProfilesById.get(employee.getUserProfile().getUserProfileId());
            if (resolvedProfile != null) {
                employee.setUserProfile(resolvedProfile);
            }
        });
        return employees;
    }

    private void ensureInternalOnboardingApprovalSatisfied(UUID employeeId) {
        internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId)
                .filter(candidate -> requiresApproval(candidate.roleCode()))
                .ifPresent(candidate -> {
                    ApprovalRequest latestApprovalRequest = internalEmployeeApprovalPortOut
                            .findLatestInternalEmployeeApprovalRequest(employeeId)
                            .orElseThrow(() -> new ConflictException(
                                    "Internal employee activation requires an approved onboarding request"
                            ));
                    if (latestApprovalRequest.getStatus() != ApprovalRequestStatus.APPROVED) {
                        throw new ConflictException(
                                "Use the internal employee approval request flow before activating this employee"
                        );
                    }
                });
    }

    private boolean requiresApproval(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode).requiresEmployeeRecord();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
