package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.CustomerType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCustomerRequest {

    private String customerCode;
    private CustomerType customerType;
}
