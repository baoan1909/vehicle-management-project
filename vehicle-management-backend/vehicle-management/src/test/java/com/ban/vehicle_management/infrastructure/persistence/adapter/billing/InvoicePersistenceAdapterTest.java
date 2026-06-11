package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.infrastructure.mapper.billing.InvoicePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class InvoicePersistenceAdapterTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LostCardReportRepository lostCardReportRepository;

    @Mock
    private InvoicePersistenceMapper invoicePersistenceMapper;

    @InjectMocks
    private InvoicePersistenceAdapter invoicePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        InvoiceEntity entity = new InvoiceEntity();

        when(invoicePersistenceMapper.toEntity(invoice)).thenReturn(entity);
        when(invoiceRepository.saveAndFlush(entity)).thenReturn(entity);
        when(invoicePersistenceMapper.toDomain(entity)).thenReturn(invoice);

        Invoice savedInvoice = invoicePersistenceAdapter.save(invoice);

        assertEquals(invoice, savedInvoice);
        verify(invoiceRepository).saveAndFlush(entity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity entity = new InvoiceEntity();
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(entity));
        when(invoicePersistenceMapper.toDomain(entity)).thenReturn(invoice);

        Optional<Invoice> result = invoicePersistenceAdapter.findById(invoiceId);

        assertTrue(result.isPresent());
        assertEquals(invoiceId, result.get().getInvoiceId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        InvoiceEntity firstEntity = new InvoiceEntity();
        InvoiceEntity secondEntity = new InvoiceEntity();
        Invoice firstInvoice = new Invoice();
        Invoice secondInvoice = new Invoice();
        Instant fromDate = Instant.parse("2026-06-01T00:00:00Z");
        Instant toDate = Instant.parse("2026-06-30T23:59:59Z");

        when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(invoicePersistenceMapper.toDomain(firstEntity)).thenReturn(firstInvoice);
        when(invoicePersistenceMapper.toDomain(secondEntity)).thenReturn(secondInvoice);

        List<Invoice> result = invoicePersistenceAdapter.findAll(
                UUID.randomUUID(),
                null,
                null,
                null,
                InvoiceStatus.UNPAID,
                fromDate,
                toDate,
                "INV"
        );

        assertEquals(2, result.size());
        assertEquals(firstInvoice, result.get(0));
        assertEquals(secondInvoice, result.get(1));
    }

    @Test
    void shouldDelegateExistsCustomerById() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsCustomerById(customerId);

        assertTrue(exists);
        verify(customerRepository).existsById(customerId);
    }

    @Test
    void shouldDelegateExistsParkingSessionById() {
        UUID parkingSessionId = UUID.randomUUID();
        when(parkingSessionRepository.existsById(parkingSessionId)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsParkingSessionById(parkingSessionId);

        assertTrue(exists);
        verify(parkingSessionRepository).existsById(parkingSessionId);
    }

    @Test
    void shouldDelegateExistsSubscriptionById() {
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsSubcriptionById(subscriptionId);

        assertTrue(exists);
        verify(subscriptionRepository).existsById(subscriptionId);
    }

    @Test
    void shouldDelegateExistsLostCardReportById() {
        UUID lostCardReportId = UUID.randomUUID();
        when(lostCardReportRepository.existsById(lostCardReportId)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsLostCardReportById(lostCardReportId);

        assertTrue(exists);
        verify(lostCardReportRepository).existsById(lostCardReportId);
    }

    @Test
    void shouldDelegateExistsByParkingSessionIdAndStatusIn() {
        UUID parkingSessionId = UUID.randomUUID();
        List<InvoiceStatus> statuses = List.of(InvoiceStatus.UNPAID, InvoiceStatus.PAID);

        when(invoiceRepository.existsByParkingSessionIdAndStatusIn(parkingSessionId, statuses)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsByParkingSessionIdAndStatusIn(parkingSessionId, statuses);

        assertTrue(exists);
        verify(invoiceRepository).existsByParkingSessionIdAndStatusIn(parkingSessionId, statuses);
    }

    @Test
    void shouldDelegateExistsBySubscriptionIdAndStatusIn() {
        UUID subscriptionId = UUID.randomUUID();
        List<InvoiceStatus> statuses = List.of(InvoiceStatus.UNPAID, InvoiceStatus.PAID);

        when(invoiceRepository.existsBySubscriptionIdAndStatusIn(subscriptionId, statuses)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsBySubscriptionIdAndStatusIn(subscriptionId, statuses);

        assertTrue(exists);
        verify(invoiceRepository).existsBySubscriptionIdAndStatusIn(subscriptionId, statuses);
    }

    @Test
    void shouldDelegateExistsByLostCardReportIdAndStatusIn() {
        UUID lostCardReportId = UUID.randomUUID();
        List<InvoiceStatus> statuses = List.of(InvoiceStatus.UNPAID, InvoiceStatus.PAID);

        when(invoiceRepository.existsByLostCardReportIdAndStatusIn(lostCardReportId, statuses)).thenReturn(true);

        boolean exists = invoicePersistenceAdapter.existsByLostCardReportIdAndStatusIn(lostCardReportId, statuses);

        assertTrue(exists);
        verify(invoiceRepository).existsByLostCardReportIdAndStatusIn(lostCardReportId, statuses);
    }
}
