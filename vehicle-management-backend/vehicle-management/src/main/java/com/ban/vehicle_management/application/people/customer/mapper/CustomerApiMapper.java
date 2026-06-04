package com.ban.vehicle_management.application.people.customer.mapper;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerApiMapper {

    CustomerAdminResponse toAdminResponse(Customer customer);

    List<CustomerAdminResponse> toAdminResponses(List<Customer> customers);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
