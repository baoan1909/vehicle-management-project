package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport;

import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.card.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.billing.invoice.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.people.customer.CustomerEntity;
import com.ban.vehicle_management.shared.enumeration.LostCardReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lost_card_reports", schema = "access_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LostCardReportEntity extends AuditableEntity {

    @Id
    @Column(name = "lost_card_report_id", nullable = false)
    private UUID lostCardReportId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", referencedColumnName = "card_id", insertable = false, updatable = false)
    private CardEntity card;

    @Column(name = "customer_id")
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", insertable = false, updatable = false)
    private CustomerEntity customer;

    @Column(name = "parking_session_id")
    private UUID parkingSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_session_id", referencedColumnName = "parking_session_id", insertable = false, updatable = false)
    private ParkingSessionEntity parkingSession;

    @Column(name = "notification_time", nullable = false)
    private Instant notificationTime;

    @Column(name = "time_of_lost", nullable = false)
    private Instant timeOfLost;

    @Column(name = "ticket_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "lost_card_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lostCardFee;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "reporter_phone")
    private String reporterPhone;

    @Column(name = "identify_card")
    private String identifyCard;

    @Column(name = "registration_license")
    private String registrationLicense;

    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LostCardReportStatus status;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity resolvedByAccount;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "lostCardReport")
    private Set<InvoiceEntity> invoices = new HashSet<>();

}
