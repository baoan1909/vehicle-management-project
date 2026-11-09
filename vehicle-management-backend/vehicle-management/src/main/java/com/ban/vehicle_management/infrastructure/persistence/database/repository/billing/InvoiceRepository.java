package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import java.math.BigDecimal;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard.RevenueByDateProjection;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {

    @Query(value = """
        select invoice_id from billing.invoices
        where customer_id=:customerId and status='UNPAID' limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionBlockingInvoiceId(@Param("customerId") UUID customerId);
    @Query(value = """
            select coalesce(sum(final_amount), 0)
            from billing.invoices
            where status = 'PAID' and paid_at >= :from and paid_at < :to
            """, nativeQuery = true)
    BigDecimal sumPaidRevenue(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select cast(paid_at at time zone cast(:zoneId as text) as date) as "date",
                   coalesce(sum(final_amount), 0) as "revenue"
            from billing.invoices
            where status = 'PAID' and paid_at >= :from and paid_at < :to
            group by "date"
            order by "date"
            """, nativeQuery = true)
    List<RevenueByDateProjection> findRevenueByDate(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("zoneId") String zoneId);

    @Query("""
            select invoice
            from InvoiceEntity invoice
            left join invoice.customer customer
            left join customer.userProfile profile
            left join profile.account account
            where invoice.invoiceId = :invoiceId
              and (invoice.customerId is null or account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
            """)
    Optional<InvoiceEntity> findBusinessVisibleById(@Param("invoiceId") UUID invoiceId);

    boolean existsByParkingSessionIdAndStatusIn(UUID parkingSessionId, Collection<InvoiceStatus> statuses);

    boolean existsBySubscriptionIdAndStatusIn(UUID subcriptionId, Collection<InvoiceStatus> statuses);

    boolean existsBySubscriptionIdAndPurposeAndStatusIn(
            UUID subscriptionId,
            InvoicePurpose purpose,
            Collection<InvoiceStatus> statuses
    );

    boolean existsByLostCardReportIdAndStatusIn(UUID lostCardReportId, Collection<InvoiceStatus> statuses);

    Optional<InvoiceEntity> findFirstBySubscriptionIdAndStatus(UUID subscriptionId, InvoiceStatus status);

    Optional<InvoiceEntity> findFirstBySubscriptionIdAndPurposeAndStatus(
            UUID subscriptionId,
            InvoicePurpose purpose,
            InvoiceStatus status
    );

    Optional<InvoiceEntity> findFirstBySubscriptionIdAndStatusIn(
            UUID subscriptionId,
            Collection<InvoiceStatus> statuses
    );

    Optional<InvoiceEntity> findFirstByParkingSessionIdAndStatusIn(
            UUID parkingSessionId,
            Collection<InvoiceStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceEntity i where i.invoiceId = :invoiceId")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("invoiceId") UUID invoiceId);

    @Query("""
        SELECT account.accountId
        FROM InvoiceEntity invoice
        JOIN CustomerEntity customer
          ON customer.customerId = invoice.customerId
        JOIN AccountEntity account
          ON account.userProfileId = customer.userProfileId
        WHERE invoice.invoiceId = :invoiceId
        """)
    Optional<UUID> findCustomerAccountIdByInvoiceId(@Param("invoiceId") UUID invoiceId);
}


