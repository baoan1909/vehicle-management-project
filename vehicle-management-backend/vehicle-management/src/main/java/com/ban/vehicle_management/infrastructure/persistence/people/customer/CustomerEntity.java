package com.ban.vehicle_management.infrastructure.persistence.people.customer;

import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.billing.invoice.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.operations.supportticket.SupportTicketEntity;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.people.customervehicle.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.people.userprofile.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity extends AuditableEntity {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "user_profile_id", nullable = false, unique = true)
    private UUID userProfileId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", referencedColumnName = "user_profile_id", insertable = false, updatable = false)
    private UserProfileEntity userProfile;

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private CustomerApprovalStatus approvalStatus;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity approvedByAccount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @OneToMany(mappedBy = "customer")
    private Set<CustomerVehicleEntity> customerVehicles = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<LostCardReportEntity> lostCardReports = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<ParkingSessionEntity> parkingSessions = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<InvoiceEntity> invoices = new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<SupportTicketEntity> supportTickets = new HashSet<>();

}
