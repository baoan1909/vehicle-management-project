package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.CustomerType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCustomerRequest {

    private UUID userProfileId;
    private String customerCode;
    private CustomerType customerType;
}
