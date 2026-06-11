package com.ban.vehicle_management.application.billing.invoice.usecase;

import com.ban.vehicle_management.application.billing.invoice.authorization.InvoiceAccessGuard;
import com.ban.vehicle_management.application.billing.invoice.port.in.InvoicePortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceUseCaseImpl implements InvoicePortIn {
    private static  final List<InvoiceStatus> ACTIVE_INVOICE_STATUSES = List.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PAID
    );

    private static final DateTimeFormatter INVOICE_NO_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(DateTimeUtils.VIETNAM_ZONE);

    private final InvoicePortOut invoicePortOut;
    private final InvoiceAccessGuard invoiceAccessGuard;
    private  final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public  InvoiceUseCaseImpl(
            InvoicePortOut invoicePortOut,
            InvoiceAccessGuard invoiceAccessGuard
    ){
        this.invoicePortOut = invoicePortOut;
        this.invoiceAccessGuard = invoiceAccessGuard;
    }

    @Override
    @Transactional
    public Invoice createInvoice(Invoice invoice){
        invoiceAccessGuard.ensureCanCreate();

        invoice.setInvoiceId(UUID.randomUUID());
        validateRelatedData(invoice);
        validateDuplicateActiveSource(invoice);

        Instant now = Instant.now();
        invoicePolicy.initializeNewInvoice(invoice, generateInvoiceNo(invoice.getInvoiceId(), now), now);

        return  invoicePortOut.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID invoiceId){
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        invoiceAccessGuard.ensureCanRead(invoice);
        return  invoice;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(
            UUID customerId,
            UUID parkingSessionId,
            UUID subcriptionId,
            UUID lostCardReportId,
            InvoiceStatus status,
            Instant fromDate,
            Instant toDate,
            String keyword
    ){
        UUID readableCustomerId = invoiceAccessGuard.resolveCustomerIdForList(customerId);

        return invoicePortOut.findAll(
                readableCustomerId,
                parkingSessionId,
                subcriptionId,
                lostCardReportId,
                status,
                fromDate,
                toDate,
                normalizeKeyword(keyword)
        );
    }

    @Override
    @Transactional
    public Invoice cancelInvoice(UUID invoiceId){
        invoiceAccessGuard.ensureCanCancel();
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        invoicePolicy.cancel(invoice);
        return invoicePortOut.save(invoice);
    }

    private Invoice findInvoiceOrThrow(UUID invoiceId){
        return invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private void validateRelatedData(Invoice invoice){
        if (invoice.getCustomerId() != null && !invoicePortOut.existsCustomerById(invoice.getCustomerId())){
            throw new NotFoundException("Customer not found");
        }

        if(invoice.getParkingSessionId() != null && !invoicePortOut.existsParkingSessionById(invoice.getParkingSessionId())) {
            throw new NotFoundException("Parking Session not found");
        }

        if (invoice.getLostCardReportId() != null && !invoicePortOut.existsLostCardReportById(invoice.getLostCardReportId())){
            throw new NotFoundException("Lost card report not found");
        }

        if (invoice.getSubscriptionId() != null && !invoicePortOut.existsSubcriptionById(invoice.getSubscriptionId())){
            throw new NotFoundException("Subcription not found");
        }
    }

    private void validateDuplicateActiveSource(Invoice invoice){
        if (invoice.getParkingSessionId() != null
                && invoicePortOut.existsByParkingSessionIdAndStatusIn(
                invoice.getParkingSessionId(),
                ACTIVE_INVOICE_STATUSES
        )){
            throw new ConflictException("Active invoice already exists for parking session");
        }

        if (invoice.getSubscriptionId() != null
                && invoicePortOut.existsBySubscriptionIdAndStatusIn(
                invoice.getSubscriptionId(),
                ACTIVE_INVOICE_STATUSES
        )){
            throw new ConflictException("Active invoice already exists for subcription");
        }

        if (invoice.getLostCardReportId() != null
                && invoicePortOut.existsByLostCardReportIdAndStatusIn(
                invoice.getLostCardReportId(),
                ACTIVE_INVOICE_STATUSES
        )){
            throw new ConflictException("Active invoice already exists for lost card report");
        }
    }

    private String generateInvoiceNo(UUID invoiceId, Instant now){
        String suffix = invoiceId.toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        return "INV-" + INVOICE_NO_TIME_FORMATTER.format(now)+"-"+ suffix;

    }

    private String normalizeKeyword(String keyword){
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
