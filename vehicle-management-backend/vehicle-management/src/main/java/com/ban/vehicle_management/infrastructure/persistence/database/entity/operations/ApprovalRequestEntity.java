package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "approval_requests", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestEntity extends AuditableEntity {

    @Id
    @Column(name = "approval_request_id", nullable = false)
    private UUID approvalRequestId;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "target_schema", nullable = false)
    private String targetSchema;

    @Column(name = "target_table", nullable = false)
    private String targetTable;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalRequestStatus status;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity requestedByAccount;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity approvedByAccount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "note")
    private String note;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> requestData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_data", columnDefinition = "jsonb")
    private Map<String, String> decisionData;

}


