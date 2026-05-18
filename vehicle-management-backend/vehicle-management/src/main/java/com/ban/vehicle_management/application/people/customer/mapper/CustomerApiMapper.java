package com.ban.vehicle_management.application.people.customer.mapper;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CreateCustomerRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerApiMapper {

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toDomain(CreateCustomerRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toDomain(UpdateCustomerRequest request);

    CustomerAdminResponse toAdminResponse(Customer customer);

    List<CustomerAdminResponse> toAdminResponses(List<Customer> customers);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
