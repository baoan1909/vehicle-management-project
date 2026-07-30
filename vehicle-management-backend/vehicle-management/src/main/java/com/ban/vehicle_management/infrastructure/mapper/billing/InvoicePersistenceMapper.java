package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoicePersistenceMapper {

    InvoiceEntity toEntity(Invoice domain);

    Invoice toDomain(InvoiceEntity entity);

    List<Invoice> toDomains(List<InvoiceEntity> entities);
}


