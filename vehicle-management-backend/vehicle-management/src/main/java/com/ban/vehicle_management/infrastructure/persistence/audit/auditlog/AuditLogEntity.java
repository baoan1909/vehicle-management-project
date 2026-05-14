package com.ban.vehicle_management.infrastructure.persistence.audit.auditlog;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs", schema = "audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity extends AuditableEntity {

    @Id
    @Column(name = "audit_log_id", nullable = false)
    private UUID auditLogId;

    @Column(name = "actor_account_id")
    private UUID actorAccountId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "target_schema")
    private String targetSchema;

    @Column(name = "target_table")
    private String targetTable;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "jsonb")
    private Map<String, Object> oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb")
    private Map<String, Object> newData;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

}
