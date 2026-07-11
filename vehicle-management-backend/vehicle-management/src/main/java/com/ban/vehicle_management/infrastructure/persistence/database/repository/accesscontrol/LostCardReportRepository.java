package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostCardReportRepository extends
        JpaRepository<LostCardReportEntity, UUID>,
        JpaSpecificationExecutor<LostCardReportEntity> {

    boolean existsByCardIdAndStatus(UUID cardId, LostCardReportStatus status);

    boolean existsByParkingSessionIdAndStatus(UUID parkingSessionId, LostCardReportStatus status);

    boolean existsByCardId(UUID cardId);

    long countByStatus(LostCardReportStatus status);

    List<LostCardReportEntity> findByStatusAndContextAndNotificationTimeBetween(
            LostCardReportStatus status,
            LostCardReportContext context,
            Instant fromDate,
            Instant toDate
    );

    @Query("""
        select new com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult(
            report.lostCardReportId,
            report.cardId,
            report.customerId,
            report.parkingSessionId,
            report.subscriptionId,
            coalesce(parkingSession.licensePlateIn, customerVehicle.licensePlate),
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
        left join InvoiceEntity invoice on invoice.lostCardReportId = report.lostCardReportId
        where (:status is null or report.status = :status)
          and (:context is null or report.context = :context)
          and (:customerId is null or report.customerId = :customerId)
          and (:cardId is null or report.cardId = :cardId)
          and (:parkingSessionId is null or report.parkingSessionId = :parkingSessionId)
          and (:subscriptionId is null or report.subscriptionId = :subscriptionId)
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
              :keyword is null
              or lower(coalesce(report.reporterName, '')) like :keyword
              or lower(coalesce(report.reporterPhone, '')) like :keyword
              or lower(coalesce(report.identifyCard, '')) like :keyword
              or lower(coalesce(report.registrationLicense, '')) like :keyword
              or lower(coalesce(parkingSession.licensePlateIn, '')) like :keyword
              or lower(coalesce(customerVehicle.licensePlate, '')) like :keyword
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
            @Param("keyword") String keyword
    );

    @Query("""
        select count(report)
        from LostCardReportEntity report
        where report.status = :status
          and report.resolvedAt >= :fromDate
          and report.resolvedAt <= :toDate
        """)
    long countByStatusAndResolvedAtBetween(
            @Param("status") LostCardReportStatus status,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    @Query("""
        select count(distinct report.lostCardReportId)
        from LostCardReportEntity report
        join InvoiceEntity invoice on invoice.lostCardReportId = report.lostCardReportId
        where report.status = :reportStatus
          and invoice.status = :invoiceStatus
        """)
    long countByReportStatusAndInvoiceStatus(
            @Param("reportStatus") LostCardReportStatus reportStatus,
            @Param("invoiceStatus") InvoiceStatus invoiceStatus
    );

    @Query("""
        select count(distinct report.cardId)
        from LostCardReportEntity report
        join report.card card
        where card.status = :cardStatus
        """)
    long countDistinctCardsByCardStatus(@Param("cardStatus") CardStatus cardStatus);
}
