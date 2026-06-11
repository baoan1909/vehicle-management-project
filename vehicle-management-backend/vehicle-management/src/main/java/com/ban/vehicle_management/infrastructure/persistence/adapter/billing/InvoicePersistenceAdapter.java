package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.mapper.billing.InvoicePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.billing.InvoiceSpecifications;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InvoicePersistenceAdapter implements InvoicePortOut {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LostCardReportRepository lostCardReportRepository;
    private final InvoicePersistenceMapper invoicePersistenceMapper;

    public InvoicePersistenceAdapter(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            ParkingSessionRepository parkingSessionRepository,
            SubscriptionRepository subscriptionRepository,
            LostCardReportRepository lostCardReportRepository,
            InvoicePersistenceMapper invoicePersistenceMapper
    ){
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.lostCardReportRepository = lostCardReportRepository;
        this.invoicePersistenceMapper = invoicePersistenceMapper;
    }

    @Override
    public Invoice save(Invoice invoice){
        InvoiceEntity savedEntity = invoiceRepository.saveAndFlush(invoicePersistenceMapper.toEntity(invoice));
        return invoicePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Invoice> findById(UUID invoiceId){
        return invoiceRepository.findById(invoiceId)
                .map(invoicePersistenceMapper::toDomain);
    }

    @Override
    public List<Invoice> findAll(
            UUID customerId,
            UUID parkingSessionId,
            UUID subcriptionId,
            UUID lostCardReportId,
            InvoiceStatus status,
            Instant fromDate,
            Instant toDate,
            String keyword
    ){
        return invoiceRepository.findAll(InvoiceSpecifications.withFilters(
                customerId,
                parkingSessionId,
                subcriptionId,
                lostCardReportId,
                status,
                fromDate,
                toDate,
                keyword
            )
        ).stream()
                .map(invoicePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public  boolean existsCustomerById(UUID customerId){
        return  customerRepository.existsById(customerId);
    }

    @Override
    public  boolean existsParkingSessionById(UUID parkingSessionId){
        return  parkingSessionRepository.existsById(parkingSessionId);
    }

    @Override
    public  boolean existsSubcriptionById(UUID subcriptionId){
        return  subscriptionRepository.existsById(subcriptionId);
    }

    @Override
    public  boolean existsLostCardReportById(UUID lostCardReportId){
        return  lostCardReportRepository.existsById(lostCardReportId);
    }

    @Override
    public boolean existsByParkingSessionIdAndStatusIn(UUID parkingSessionId, Collection<InvoiceStatus> statuses) {
        return invoiceRepository.existsByParkingSessionIdAndStatusIn(parkingSessionId, statuses);
    }

    @Override
    public boolean existsBySubscriptionIdAndStatusIn(UUID subscriptionId, Collection<InvoiceStatus> statuses) {
        return invoiceRepository.existsBySubscriptionIdAndStatusIn(subscriptionId, statuses);
    }

    @Override
    public boolean existsByLostCardReportIdAndStatusIn(UUID lostCardReportId, Collection<InvoiceStatus> statuses) {
        return invoiceRepository.existsByLostCardReportIdAndStatusIn(lostCardReportId, statuses);
    }
}
