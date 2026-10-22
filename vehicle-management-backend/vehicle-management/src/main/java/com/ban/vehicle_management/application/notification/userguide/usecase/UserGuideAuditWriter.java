package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserGuideAuditWriter {

    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;

    public UserGuideAuditWriter(AuditLogPortOut auditLogPortOut, CurrentAccountPortIn currentAccountPortIn) {
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public void record(String action, String targetTable, UUID targetId, Map<String, Object> newData) {
        record(action, targetTable, targetId, null, newData);
    }

    public void record(
            String action,
            String targetTable,
            UUID targetId,
            Map<String, Object> oldData,
            Map<String, Object> newData
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("notification");
        auditLog.setTargetTable(targetTable);
        auditLog.setTargetId(targetId);
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
