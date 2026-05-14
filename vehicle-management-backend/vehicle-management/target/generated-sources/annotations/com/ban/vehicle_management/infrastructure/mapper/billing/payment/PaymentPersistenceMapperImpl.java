package com.ban.vehicle_management.infrastructure.mapper.billing.payment;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.infrastructure.persistence.billing.payment.PaymentEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class PaymentPersistenceMapperImpl implements PaymentPersistenceMapper {

    @Override
    public PaymentEntity toEntity(Payment domain) {
        if ( domain == null ) {
            return null;
        }

        PaymentEntity paymentEntity = new PaymentEntity();

        paymentEntity.setPaymentId( domain.getPaymentId() );
        paymentEntity.setInvoiceId( domain.getInvoiceId() );
        paymentEntity.setPaymentMethod( domain.getPaymentMethod() );
        paymentEntity.setAmount( domain.getAmount() );
        paymentEntity.setTransactionRef( domain.getTransactionRef() );
        paymentEntity.setStatus( domain.getStatus() );
        paymentEntity.setPaidAt( domain.getPaidAt() );
        paymentEntity.setReceivedBy( domain.getReceivedBy() );
        paymentEntity.setNote( domain.getNote() );

        return paymentEntity;
    }

    @Override
    public Payment toDomain(PaymentEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Payment payment = new Payment();

        payment.setPaymentId( entity.getPaymentId() );
        payment.setInvoiceId( entity.getInvoiceId() );
        payment.setPaymentMethod( entity.getPaymentMethod() );
        payment.setAmount( entity.getAmount() );
        payment.setTransactionRef( entity.getTransactionRef() );
        payment.setStatus( entity.getStatus() );
        payment.setPaidAt( entity.getPaidAt() );
        payment.setReceivedBy( entity.getReceivedBy() );
        payment.setNote( entity.getNote() );

        return payment;
    }
}
