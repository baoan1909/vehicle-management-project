package com.ban.vehicle_management.application.people.customer.port.in;

import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerAdminProfilePortIn {

    CustomerAdminProfileResult updateCustomerAdminProfile(UUID customerId, UpdateCustomerAdminProfileCommand command);

    CustomerAdminProfileResult uploadCustomerAvatar(UUID customerId, MultipartFile file);

    CustomerAdminProfileResult deleteCustomerAvatar(UUID customerId);
}
