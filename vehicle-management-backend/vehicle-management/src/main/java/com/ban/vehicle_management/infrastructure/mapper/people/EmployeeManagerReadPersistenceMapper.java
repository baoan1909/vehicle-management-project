package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.AuditLogRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EmployeeManagerReadPersistenceMapper {

    ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(DISPLAY_ZONE);

    @Mapping(target = "timeRange", expression = "java(formatTimeRange(projection.getStartTime(), projection.getEndTime()))")
    @Mapping(target = "locationName", source = "locationName", qualifiedByName = "defaultLocationName")
    @Mapping(target = "roleInShift", constant = "OPERATOR")
    EmployeeRecentShiftResult toRecentShiftResult(
            ShiftAssignmentRepository.RecentEmployeeShiftProjection projection
    );

    @Mapping(target = "eventType", expression = "java(\"EMPLOYEE_\" + defaultText(projection.getAction(), \"UPDATED\"))")
    @Mapping(target = "title", constant = "Cập nhật hồ sơ nhân viên")
    @Mapping(target = "description", expression = "java(\"Thao tác \" + defaultText(projection.getAction(), \"UPDATED\") + \" trên hồ sơ nhân viên.\")")
    @Mapping(target = "actorName", expression = "java(actorName(projection.getActorFullName(), projection.getActorUsername()))")
    EmployeeActivityTimelineResult toAuditTimelineResult(
            AuditLogRepository.EmployeeAuditTimelineProjection projection
    );

    @Mapping(target = "eventType", constant = "ACCOUNT_STATUS_CHANGED")
    @Mapping(target = "title", constant = "Cập nhật trạng thái tài khoản")
    @Mapping(target = "description", expression = "java(accountStatusDescription(projection.getNewStatus(), projection.getReason()))")
    @Mapping(target = "actorName", expression = "java(actorName(projection.getActorFullName(), projection.getActorUsername()))")
    EmployeeActivityTimelineResult toAccountStatusTimelineResult(
            AccountStatusHistoryRepository.EmployeeAccountStatusTimelineProjection projection
    );

    @Mapping(target = "eventType", expression = "java(\"INTERNAL_EMPLOYEE_ONBOARDING_\" + statusLabel(projection.getStatus()))")
    @Mapping(target = "title", constant = "Duyệt hồ sơ nội bộ")
    @Mapping(target = "description", expression = "java(approvalDescription(projection.getStatus(), projection.getNote()))")
    @Mapping(target = "actorName", expression = "java(actorName(projection.getActorFullName(), projection.getActorUsername()))")
    EmployeeActivityTimelineResult toApprovalTimelineResult(
            ApprovalRequestRepository.EmployeeApprovalTimelineProjection projection
    );

    default String formatTimeRange(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            return "-";
        }
        return TIME_FORMATTER.format(startTime) + " - " + TIME_FORMATTER.format(endTime);
    }

    @Named("defaultLocationName")
    default String defaultLocationName(String locationName) {
        return defaultText(locationName, "-");
    }

    default String accountStatusDescription(AccountStatus newStatus, String reason) {
        return "Tài khoản chuyển sang trạng thái " + statusLabel(newStatus) + reasonSuffix(reason);
    }

    default String approvalDescription(ApprovalRequestStatus status, String note) {
        return "Yêu cầu onboarding nội bộ ở trạng thái " + statusLabel(status) + reasonSuffix(note);
    }

    default String actorName(String fullName, String username) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return "System";
    }

    default String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    default String statusLabel(Enum<?> status) {
        return status == null ? "UNKNOWN" : status.name();
    }

    private static String reasonSuffix(String reason) {
        return reason == null || reason.isBlank() ? "." : ". Ghi chú: " + reason;
    }
}
