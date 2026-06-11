package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.customer.mapper.CustomerAdminProfileApiMapper;
import com.ban.vehicle_management.application.people.customer.mapper.CustomerApiMapper;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerAdminProfilePortIn;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerPortIn;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CustomerFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminProfileResponse;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/customers")
public class CustomerController {

    private final CustomerPortIn customerPortIn;
    private final CustomerAdminProfilePortIn customerAdminProfilePortIn;
    private final CustomerApiMapper customerApiMapper;
    private final CustomerAdminProfileApiMapper customerAdminProfileApiMapper;

    public CustomerController(
            CustomerPortIn customerPortIn,
            CustomerAdminProfilePortIn customerAdminProfilePortIn,
            CustomerApiMapper customerApiMapper,
            CustomerAdminProfileApiMapper customerAdminProfileApiMapper
    ) {
        this.customerPortIn = customerPortIn;
        this.customerAdminProfilePortIn = customerAdminProfilePortIn;
        this.customerApiMapper = customerApiMapper;
        this.customerAdminProfileApiMapper = customerAdminProfileApiMapper;
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_READ_ALL')")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> getCustomerById(@PathVariable UUID customerId) {
        Customer customer = customerPortIn.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer successfully",
                customerApiMapper.toAdminResponse(customer)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_READ_ALL')")
    public ResponseEntity<ApiResponse<List<CustomerAdminResponse>>> getCustomers(
            @ModelAttribute CustomerFilterRequest request
    ) {
        List<Customer> customers = customerPortIn.getCustomers(
                request.status(),
                request.approvalStatus(),
                request.customerType(),
                request.keyword()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customers successfully",
                customerApiMapper.toAdminResponses(customers)
        ));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CustomerAdminProfileResponse>> updateCustomer(
            @PathVariable UUID customerId,
            @RequestBody UpdateCustomerAdminProfileRequest request
    ) {
        CustomerAdminProfileResult updatedCustomerProfile = customerAdminProfilePortIn.updateCustomerAdminProfile(
                customerId,
                customerAdminProfileApiMapper.toUpdateCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer updated successfully",
                customerAdminProfileApiMapper.toResponse(updatedCustomerProfile)
        ));
    }

    @PatchMapping("/{customerId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> activateCustomer(@PathVariable UUID customerId) {
        Customer activatedCustomer = customerPortIn.activateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer activated successfully",
                customerApiMapper.toAdminResponse(activatedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/inactivate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> inactivateCustomer(@PathVariable UUID customerId) {
        Customer inactivatedCustomer = customerPortIn.inactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer inactivated successfully",
                customerApiMapper.toAdminResponse(inactivatedCustomer)
        ));
    }
}
