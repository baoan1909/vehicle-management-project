package com.ban.vehicle_management.application.people.customer.mapper;

import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = UserProfileApiMapper.class)
public interface CustomerApiMapper {

    @Mapping(target = "approvedAt", source = "approvedAt", qualifiedByName = "formatCustomerInstant")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatCustomerInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatCustomerInstant")
    CustomerAdminResponse toAdminResponse(Customer customer);

    List<CustomerAdminResponse> toAdminResponses(List<Customer> customers);

    @Named("formatCustomerInstant")
    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
