package com.ban.vehicle_management.application.people.employee.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
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
    private final EmployeePolicy employeePolicy = new EmployeePolicy();

    public EmployeeUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeeAccessGuard employeeAccessGuard,
            EmployeePortOut employeePortOut,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            UserProfileAvatarPortIn userProfileAvatarPortIn
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.employeeAccessGuard = employeeAccessGuard;
        this.employeePortOut = employeePortOut;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.userProfileAvatarPortIn = userProfileAvatarPortIn;
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

        return withResolvedAvatarUrl(employeePortOut.save(existingEmployee));
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
        return withResolvedAvatarUrl(employeePortOut.save(employee));
    }

    @Override
    @Transactional
    public Employee inactivateEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);
        employeePolicy.inactivate(employee);
        return withResolvedAvatarUrl(employeePortOut.save(employee));
    }

    @Override
    @Transactional
    public Employee suspendEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeeAccessGuard.ensureCanManage(employee);
        employeePolicy.suspend(employee);
        return withResolvedAvatarUrl(employeePortOut.save(employee));
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
