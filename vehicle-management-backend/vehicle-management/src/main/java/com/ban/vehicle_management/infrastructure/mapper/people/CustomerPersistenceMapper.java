package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserProfilePersistenceMapper.class)
public interface CustomerPersistenceMapper {

    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "approvedByAccount", ignore = true)
    @Mapping(target = "customerVehicles", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "lostCardReports", ignore = true)
    @Mapping(target = "parkingSessions", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "supportTickets", ignore = true)
    CustomerEntity toEntity(Customer domain);

    @Mapping(target = "accountEmail", expression = "java(resolveAccountEmail(entity))")
    Customer toDomain(CustomerEntity entity);

    default String resolveAccountEmail(CustomerEntity entity) {
        if (entity == null
                || entity.getUserProfile() == null
                || entity.getUserProfile().getAccount() == null) {
            return null;
        }
        return entity.getUserProfile().getAccount().getEmail();
    }
}


