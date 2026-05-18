package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.customer.mapper.CustomerApiMapper;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerPortIn;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.ApproveCustomerRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CreateCustomerRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/customers")
public class CustomerController {

    private final CustomerPortIn customerPortIn;
    private final CustomerApiMapper customerApiMapper;

    public CustomerController(CustomerPortIn customerPortIn, CustomerApiMapper customerApiMapper) {
        this.customerPortIn = customerPortIn;
        this.customerApiMapper = customerApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> createCustomer(@RequestBody CreateCustomerRequest request) {
        Customer createdCustomer = customerPortIn.createCustomer(customerApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Customer created successfully",
                customerApiMapper.toAdminResponse(createdCustomer)
        ));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> getCustomerById(@PathVariable UUID customerId) {
        Customer customer = customerPortIn.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer successfully",
                customerApiMapper.toAdminResponse(customer)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAdminResponse>>> getCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) CustomerApprovalStatus approvalStatus,
            @RequestParam(required = false) CustomerType customerType,
            @RequestParam(required = false) String keyword
    ) {
        List<Customer> customers = customerPortIn.getCustomers(status, approvalStatus, customerType, keyword);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customers successfully",
                customerApiMapper.toAdminResponses(customers)
        ));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> updateCustomer(
            @PathVariable UUID customerId,
            @RequestBody UpdateCustomerRequest request
    ) {
        Customer updatedCustomer = customerPortIn.updateCustomer(customerId, customerApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer updated successfully",
                customerApiMapper.toAdminResponse(updatedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/approve")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> approveCustomer(
            @PathVariable UUID customerId,
            @RequestBody ApproveCustomerRequest request
    ) {
        Customer approvedCustomer = customerPortIn.approveCustomer(customerId, request.getApprovedBy(), request.getApprovedAt());
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer approved successfully",
                customerApiMapper.toAdminResponse(approvedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/reject")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> rejectCustomer(@PathVariable UUID customerId) {
        Customer rejectedCustomer = customerPortIn.rejectCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer rejected successfully",
                customerApiMapper.toAdminResponse(rejectedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/suspend")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> suspendCustomer(@PathVariable UUID customerId) {
        Customer suspendedCustomer = customerPortIn.suspendCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer suspended successfully",
                customerApiMapper.toAdminResponse(suspendedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/move-to-pending")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> moveCustomerToPending(@PathVariable UUID customerId) {
        Customer pendingCustomer = customerPortIn.moveCustomerToPending(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer moved to pending successfully",
                customerApiMapper.toAdminResponse(pendingCustomer)
        ));
    }

    @PatchMapping("/{customerId}/activate")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> activateCustomer(@PathVariable UUID customerId) {
        Customer activatedCustomer = customerPortIn.activateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer activated successfully",
                customerApiMapper.toAdminResponse(activatedCustomer)
        ));
    }

    @PatchMapping("/{customerId}/inactivate")
    public ResponseEntity<ApiResponse<CustomerAdminResponse>> inactivateCustomer(@PathVariable UUID customerId) {
        Customer inactivatedCustomer = customerPortIn.inactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer inactivated successfully",
                customerApiMapper.toAdminResponse(inactivatedCustomer)
        ));
    }
}
