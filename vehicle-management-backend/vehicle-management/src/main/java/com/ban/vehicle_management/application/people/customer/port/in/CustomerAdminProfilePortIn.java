package com.ban.vehicle_management.application.people.customer.port.in;

import com.ban.vehicle_management.application.people.customer.model.command.CreateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import java.util.UUID;

public interface CustomerAdminProfilePortIn {

    CustomerAdminProfileResult createCustomerAdminProfile(CreateCustomerAdminProfileCommand command);

    CustomerAdminProfileResult updateCustomerAdminProfile(UUID customerId, UpdateCustomerAdminProfileCommand command);
}
