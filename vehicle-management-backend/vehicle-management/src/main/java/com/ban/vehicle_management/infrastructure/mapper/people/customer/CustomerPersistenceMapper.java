package com.ban.vehicle_management.infrastructure.mapper.people.customer;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.persistence.people.customer.CustomerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerPersistenceMapper {

    CustomerEntity toEntity(Customer domain);

    Customer toDomain(CustomerEntity entity);
}
