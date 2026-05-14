package com.ban.vehicle_management.infrastructure.mapper.billing.invoice;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.infrastructure.persistence.billing.invoice.InvoiceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoicePersistenceMapper {

    InvoiceEntity toEntity(Invoice domain);

    Invoice toDomain(InvoiceEntity entity);
}
