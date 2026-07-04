package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.infrastructure.mapper.billing.PaymentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentPersistenceAdapterTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPersistenceMapper paymentPersistenceMapper;

    @InjectMocks
    private PaymentPersistenceAdapter paymentPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingPayment() {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        PaymentEntity entity = new PaymentEntity();

        when(paymentPersistenceMapper.toEntity(payment)).thenReturn(entity);
        when(paymentRepository.saveAndFlush(entity)).thenReturn(entity);
        when(paymentPersistenceMapper.toDomain(entity)).thenReturn(payment);

        Payment savedPayment = paymentPersistenceAdapter.save(payment);

        assertEquals(payment, savedPayment);
        verify(paymentRepository).saveAndFlush(entity);
    }

    @Test
    void shouldReturnMappedListWhenFindingByInvoiceId() {
        UUID invoiceId = UUID.randomUUID();
        PaymentEntity firstEntity = new PaymentEntity();
        PaymentEntity secondEntity = new PaymentEntity();
        Payment firstPayment = new Payment();
        Payment secondPayment = new Payment();

        when(paymentRepository.findByInvoiceIdOrderByPaidAtAsc(invoiceId)).thenReturn(List.of(firstEntity, secondEntity));
        when(paymentPersistenceMapper.toDomain(firstEntity)).thenReturn(firstPayment);
        when(paymentPersistenceMapper.toDomain(secondEntity)).thenReturn(secondPayment);

        List<Payment> result = paymentPersistenceAdapter.findByInvoiceId(invoiceId);

        assertEquals(2, result.size());
        assertEquals(firstPayment, result.get(0));
        assertEquals(secondPayment, result.get(1));
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        PaymentEntity firstEntity = new PaymentEntity();
        PaymentEntity secondEntity = new PaymentEntity();
        Payment firstPayment = new Payment();
        Payment secondPayment = new Payment();
        Instant fromDate = Instant.parse("2026-06-01T00:00:00Z");
        Instant toDate = Instant.parse("2026-06-30T23:59:59Z");

        when(paymentRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(paymentPersistenceMapper.toDomain(firstEntity)).thenReturn(firstPayment);
        when(paymentPersistenceMapper.toDomain(secondEntity)).thenReturn(secondPayment);

        List<Payment> result = paymentPersistenceAdapter.findAll(
                UUID.randomUUID(),
                PaymentMethod.BANK_TRANSFER,
                PaymentStatus.SUCCESS,
                UUID.randomUUID(),
                fromDate,
                toDate,
                "VCB"
        );

        assertEquals(2, result.size());
        assertEquals(firstPayment, result.get(0));
        assertEquals(secondPayment, result.get(1));
    }

    @Test
    void shouldDelegateExistsByInvoiceIdAndStatus() {
        UUID invoiceId = UUID.randomUUID();

        when(paymentRepository.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(true);

        boolean exists = paymentPersistenceAdapter.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS);

        assertTrue(exists);
        verify(paymentRepository).existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS);
    }

    @Test
    void shouldDelegateExistsByTransactionRefAndStatus() {
        String transactionRef = "VCB202606120001";

        when(paymentRepository.existsByTransactionRefAndStatus(transactionRef, PaymentStatus.SUCCESS)).thenReturn(true);

        boolean exists = paymentPersistenceAdapter.existsByTransactionRefAndStatus(transactionRef, PaymentStatus.SUCCESS);

        assertTrue(exists);
        verify(paymentRepository).existsByTransactionRefAndStatus(transactionRef, PaymentStatus.SUCCESS);
    }
}
