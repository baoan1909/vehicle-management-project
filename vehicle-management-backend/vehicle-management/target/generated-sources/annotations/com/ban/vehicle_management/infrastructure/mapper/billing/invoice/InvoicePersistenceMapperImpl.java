package com.ban.vehicle_management.infrastructure.mapper.billing.invoice;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.infrastructure.persistence.billing.invoice.InvoiceEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class InvoicePersistenceMapperImpl implements InvoicePersistenceMapper {

    @Override
    public InvoiceEntity toEntity(Invoice domain) {
        if ( domain == null ) {
            return null;
        }

        InvoiceEntity invoiceEntity = new InvoiceEntity();

        invoiceEntity.setCreatedAt( domain.getCreatedAt() );
        invoiceEntity.setCreatedBy( domain.getCreatedBy() );
        invoiceEntity.setUpdatedAt( domain.getUpdatedAt() );
        invoiceEntity.setUpdatedBy( domain.getUpdatedBy() );
        invoiceEntity.setInvoiceId( domain.getInvoiceId() );
        invoiceEntity.setInvoiceNo( domain.getInvoiceNo() );
        invoiceEntity.setCustomerId( domain.getCustomerId() );
        invoiceEntity.setParkingSessionId( domain.getParkingSessionId() );
        invoiceEntity.setSubscriptionId( domain.getSubscriptionId() );
        invoiceEntity.setLostCardReportId( domain.getLostCardReportId() );
        invoiceEntity.setAmount( domain.getAmount() );
        invoiceEntity.setDiscountAmount( domain.getDiscountAmount() );
        invoiceEntity.setFinalAmount( domain.getFinalAmount() );
        invoiceEntity.setStatus( domain.getStatus() );
        invoiceEntity.setIssuedAt( domain.getIssuedAt() );
        invoiceEntity.setPaidAt( domain.getPaidAt() );

        return invoiceEntity;
    }

    @Override
    public Invoice toDomain(InvoiceEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Invoice invoice = new Invoice();

        invoice.setCreatedAt( entity.getCreatedAt() );
        invoice.setCreatedBy( entity.getCreatedBy() );
        invoice.setUpdatedAt( entity.getUpdatedAt() );
        invoice.setUpdatedBy( entity.getUpdatedBy() );
        invoice.setInvoiceId( entity.getInvoiceId() );
        invoice.setInvoiceNo( entity.getInvoiceNo() );
        invoice.setCustomerId( entity.getCustomerId() );
        invoice.setParkingSessionId( entity.getParkingSessionId() );
        invoice.setSubscriptionId( entity.getSubscriptionId() );
        invoice.setLostCardReportId( entity.getLostCardReportId() );
        invoice.setAmount( entity.getAmount() );
        invoice.setDiscountAmount( entity.getDiscountAmount() );
        invoice.setFinalAmount( entity.getFinalAmount() );
        invoice.setStatus( entity.getStatus() );
        invoice.setIssuedAt( entity.getIssuedAt() );
        invoice.setPaidAt( entity.getPaidAt() );

        return invoice;
    }
}
