package com.ban.vehicle_management.domain.billing.invoice.policy;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;

import java.math.BigDecimal;
import java.time.Instant;

public class InvoicePolicy {

    public void  initializeNewInvoice(Invoice invoice, String invoiceNo, Instant issuedAt){
        requireInvoice(invoice);
        requireField(invoiceNo, "invoiceNo");
        requireField(issuedAt, "issuedAt");

        invoice.setInvoiceNo(invoiceNo);
        invoice.setIssuedAt(issuedAt);

        if(invoice.getDiscountAmount() == null){
            invoice.setDiscountAmount(BigDecimal.ZERO);
        }

        validateAmounts(invoice);

        BigDecimal finalAmount = invoice.getAmount().subtract(invoice.getDiscountAmount());
        invoice.setFinalAmount(finalAmount);

        if(finalAmount.compareTo(BigDecimal.ZERO) == 0){
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(issuedAt);
        }
        else {
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice.setPaidAt(null);
        }

        validateState(invoice);
    }

    public void cancel(Invoice invoice){
        requireInvoice(invoice);

        if(InvoiceStatus.CANCELLED.equals(invoice.getStatus())){
            return;
        }

        if(!InvoiceStatus.UNPAID.equals(invoice.getStatus())){
            throw new BadRequestException("Only unpaid invoice can be cancelled");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        validateState(invoice);
    }

    public void markPaid(Invoice invoice, Instant paidAt){
        requireInvoice(invoice);
        requireField(paidAt, "paidAt");

        if(!InvoiceStatus.UNPAID.equals(invoice.getStatus())){
            throw new BadRequestException("Only unpaid invoice can be marked as paid");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paidAt);
        validateState(invoice);
    }

    public  void validateState(Invoice invoice){
        requireInvoice(invoice);
        requireField(invoice.getInvoiceId(), "invoiceId");
        requireField(invoice.getInvoiceNo(), "invoiceNo");
        requireField(invoice.getAmount(), "amount");
        requireField(invoice.getDiscountAmount(), "dscountAmount");
        requireField(invoice.getFinalAmount(), "finalAmount");
        requireField(invoice.getStatus(), "status");
        requireField(invoice.getIssuedAt(), "issuedAt");

        validateAmounts(invoice);
        validateSource(invoice);

        BigDecimal expectedFinalAmount = invoice.getAmount().subtract(invoice.getDiscountAmount());
        if(invoice.getFinalAmount().compareTo(expectedFinalAmount) != 0){
            throw new BadRequestException("finalAmount must equal amount minus discountAmount");
        }

        if(InvoiceStatus.PAID.equals(invoice.getStatus()) && invoice.getPaidAt() == null){
            throw new BadRequestException("paid invoice must have paidAt");
        }

        if(!InvoiceStatus.PAID.equals(invoice.getStatus()) && invoice.getPaidAt() != null){
            throw new BadRequestException("Only paid invoice can have paidAt");
        }

    }

    private void validateAmounts(Invoice invoice){
        if(invoice.getAmount() == null){
            throw new BadRequestException("Amount must not be null");
        }

        if(invoice.getAmount().compareTo(BigDecimal.ZERO) < 0){
            throw new BadRequestException("amount must not be negative");
        }

        if(invoice.getDiscountAmount() == null){
            throw new BadRequestException("discountAmount must not be null");
        }

        if(invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0){
            throw new BadRequestException("discountAmount must not be negative");
        }

        if(invoice.getAmount().compareTo(invoice.getDiscountAmount()) <0 ){
            throw new BadRequestException("disocuntAmount must not be greater than amount");
        }

    }

    private void validateSource(Invoice  invoice){
        int sourceCount = 0;

        if(invoice.getParkingSessionId() != null){
            sourceCount ++;
        }

        if(invoice.getSubscriptionId() != null){
            sourceCount ++;
        }

        if(invoice.getLostCardReportId() != null){
            sourceCount ++;
        }

        if(sourceCount > 1){
            throw new BadRequestException("Invoice can belong to only one bussiness source");
        }
    }

    private void requireInvoice(Invoice invoice){
        requireField(invoice, "invoice");
    }

    private void requireField(Object value, String fieldName){
        if(value == null){
            throw  new BadRequestException(fieldName + "must not be null");
        }
    }
}
