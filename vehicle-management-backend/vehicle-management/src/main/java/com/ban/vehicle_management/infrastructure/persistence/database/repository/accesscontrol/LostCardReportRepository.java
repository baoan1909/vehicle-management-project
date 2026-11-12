package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import java.util.Collection;

public interface LostCardReportRepository extends
        JpaRepository<LostCardReportEntity, UUID>,
        JpaSpecificationExecutor<LostCardReportEntity> {

    @Query(value = """
        select lost_card_report_id from access_control.lost_card_reports
        where customer_id=:customerId and status='OPEN' limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionBlockingReportId(@Param("customerId") UUID customerId);

    @Query(value = """
        select report.lost_card_report_id as "resourceId", lot.organization_id as "organizationId",
               lot.parking_lot_id as "parkingLotId"
          from access_control.lost_card_reports report
          join parking.parking_lots lot on lot.parking_lot_id = report.parking_lot_id
         where lot.organization_id in (:organizationIds) and report.status = 'OPEN'
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findOrganizationDeletionBlockers(
            @Param("organizationIds") Collection<UUID> organizationIds);

    @Query("""
        select report
        from LostCardReportEntity report
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where report.lostCardReportId = :lostCardReportId
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
        """)
    java.util.Optional<LostCardReportEntity> findBusinessVisibleById(
            @Param("lostCardReportId") UUID lostCardReportId);

    @Query("""
        select report
        from LostCardReportEntity report
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where (:status is null or report.status = :status)
          and (:context is null or report.context = :context)
          and (:customerId is null or report.customerId = :customerId)
          and (:cardId is null or report.cardId = :cardId)
          and (:parkingSessionId is null or report.parkingSessionId = :parkingSessionId)
          and (:subscriptionId is null or report.subscriptionId = :subscriptionId)
          and report.notificationTime >= :fromDate
          and report.notificationTime <= :toDate
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
          and (
              :keyword = ''
              or lower(coalesce(report.reporterName, '')) like :keyword
              or lower(coalesce(report.reporterPhone, '')) like :keyword
              or lower(coalesce(report.identifyCard, '')) like :keyword
              or lower(coalesce(report.registrationLicense, '')) like :keyword
          )
        order by report.notificationTime desc
        """)
    List<LostCardReportEntity> findBusinessVisibleWithFilters(
            @Param("status") LostCardReportStatus status,
            @Param("context") LostCardReportContext context,
            @Param("customerId") UUID customerId,
            @Param("cardId") UUID cardId,
            @Param("parkingSessionId") UUID parkingSessionId,
            @Param("subscriptionId") UUID subscriptionId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("keyword") String keyword,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds);

    List<LostCardReportEntity> findAllByCustomerIdOrderByNotificationTimeDesc(UUID customerId);

    boolean existsByCardIdAndStatus(UUID cardId, LostCardReportStatus status);

    boolean existsByParkingSessionIdAndStatus(UUID parkingSessionId, LostCardReportStatus status);

    boolean existsByCardId(UUID cardId);

    List<LostCardReportEntity> findByStatusAndContextAndNotificationTimeBetween(
            LostCardReportStatus status,
            LostCardReportContext context,
            Instant fromDate,
            Instant toDate
    );

    @Query("""
        select new com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult(
            report.lostCardReportId,
            report.parkingLotId,
            report.cardId,
            report.customerId,
            report.parkingSessionId,
            report.subscriptionId,
            coalesce(parkingSession.licensePlateInNormalized, customerVehicle.licensePlateNormalized),
            report.notificationTime,
            report.timeOfLost,
            report.ticketPrice,
            report.lostCardFee,
            report.reporterName,
            report.reporterPhone,
            report.identifyCard,
            report.registrationLicense,
            report.context,
            report.status,
            invoice.invoiceId,
            invoice.invoiceNo,
            invoice.status,
            report.createdAt,
            report.createdBy,
            report.updatedAt,
            report.updatedBy
        )
        from LostCardReportEntity report
        left join report.parkingSession parkingSession
        left join report.subscription subscription
        left join subscription.customerVehicle customerVehicle
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        left join InvoiceEntity invoice on invoice.lostCardReportId = report.lostCardReportId
        where (:status is null or report.status = :status)
          and (:context is null or report.context = :context)
          and (:customerId is null or report.customerId = :customerId)
          and (:cardId is null or report.cardId = :cardId)
          and (:parkingSessionId is null or report.parkingSessionId = :parkingSessionId)
          and (:subscriptionId is null or report.subscriptionId = :subscriptionId)
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
          and report.timeOfLost >= :fromDate
          and report.timeOfLost <= :toDate
          and (
              invoice.invoiceId is null
              or invoice.issuedAt = (
                  select max(latestInvoice.issuedAt)
                  from InvoiceEntity latestInvoice
                  where latestInvoice.lostCardReportId = report.lostCardReportId
              )
          )
          and (
              :keyword = ''
              or lower(coalesce(report.reporterName, '')) like :keyword
              or lower(coalesce(report.reporterPhone, '')) like :keyword
              or lower(coalesce(report.identifyCard, '')) like :keyword
              or lower(coalesce(report.registrationLicense, '')) like :keyword
              or lower(coalesce(parkingSession.licensePlateIn, '')) like :keyword
              or lower(coalesce(customerVehicle.licensePlate, '')) like :keyword
              or lower(coalesce(parkingSession.licensePlateInNormalized, ''))
                    like replace(replace(replace(:keyword, '-', ''), '.', ''), ' ', '')
              or lower(coalesce(customerVehicle.licensePlateNormalized, ''))
                    like replace(replace(replace(:keyword, '-', ''), '.', ''), ' ', '')
              or lower(coalesce(invoice.invoiceNo, '')) like :keyword
          )
        order by report.notificationTime desc
        """)
    List<LostCardReportListItemResult> findListItems(
            @Param("status") LostCardReportStatus status,
            @Param("context") LostCardReportContext context,
            @Param("customerId") UUID customerId,
            @Param("cardId") UUID cardId,
            @Param("parkingSessionId") UUID parkingSessionId,
            @Param("subscriptionId") UUID subscriptionId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("keyword") String keyword,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds
    );

    @Query("""
        select count(report) from LostCardReportEntity report
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where report.status = :status
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
        """)
    long countByStatusInParkingLots(
            @Param("status") LostCardReportStatus status,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds
    );

    @Query("""
        select count(report)
        from LostCardReportEntity report
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where report.status = :status
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and report.resolvedAt >= :fromDate
          and report.resolvedAt <= :toDate
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
        """)
    long countByStatusAndResolvedAtBetween(
            @Param("status") LostCardReportStatus status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds
    );

    @Query("""
        select count(distinct report.lostCardReportId)
        from LostCardReportEntity report
        join InvoiceEntity invoice on invoice.lostCardReportId = report.lostCardReportId
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where report.status = :reportStatus
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and invoice.status = :invoiceStatus
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
        """)
    long countByReportStatusAndInvoiceStatus(
            @Param("reportStatus") LostCardReportStatus reportStatus,
            @Param("invoiceStatus") InvoiceStatus invoiceStatus,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds
    );

    @Query("""
        select count(distinct report.cardId)
        from LostCardReportEntity report
        join report.card card
        left join report.customer customer
        left join customer.userProfile customerProfile
        left join customerProfile.account customerAccount
        where card.status = :cardStatus
          and (report.customerId is null or customerAccount.status not in (
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
              com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
          and (:scopeEnabled = false or report.parkingLotId in :parkingLotIds)
        """)
    long countDistinctCardsByCardStatus(
            @Param("cardStatus") CardStatus cardStatus,
            @Param("scopeEnabled") boolean scopeEnabled,
            @Param("parkingLotIds") Set<UUID> parkingLotIds
    );
}
