package com.ban.vehicle_management.domain.audit.auditlog.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends AuditableDomainModel {

    private UUID auditLogId;
    private UUID actorAccountId;
    private String action;
    private String targetSchema;
    private String targetTable;
    private UUID targetId;
    private Map<String, Object> oldData;
    private Map<String, Object> newData;
    private String ipAddress;
    private String userAgent;
}

