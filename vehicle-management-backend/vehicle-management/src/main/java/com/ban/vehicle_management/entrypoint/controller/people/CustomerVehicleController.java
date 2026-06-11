package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.customervehicle.mapper.CustomerVehicleApiMapper;
import com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.CustomerVehicleBatchRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.CustomerVehicleFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.UpdateCustomerVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response.CustomerVehicleAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/customer-vehicles")
public class CustomerVehicleController {

    private final CustomerVehiclePortIn customerVehiclePortIn;
    private final CustomerVehicleApiMapper customerVehicleApiMapper;

    public CustomerVehicleController(
            CustomerVehiclePortIn customerVehiclePortIn,
            CustomerVehicleApiMapper customerVehicleApiMapper
    ) {
        this.customerVehiclePortIn = customerVehiclePortIn;
        this.customerVehicleApiMapper = customerVehicleApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission(" +
            "'CUSTOMER_VEHICLE_CREATE_ALL', 'CUSTOMER_VEHICLE_CREATE_OWN', " +
            "'CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<List<CustomerVehicleAdminResponse>>> applyCustomerVehicleBatch(
            @RequestBody CustomerVehicleBatchRequest request
    ) {
        List<CustomerVehicle> customerVehicles =
                customerVehiclePortIn.applyCustomerVehicleBatch(customerVehicleApiMapper.toBatchCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Customer vehicles saved successfully",
                customerVehicleApiMapper.toAdminResponses(customerVehicles)
        ));
    }

    @PutMapping("/{customerVehicleId}")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> updateCustomerVehicle(
            @PathVariable UUID customerVehicleId,
            @RequestBody UpdateCustomerVehicleRequest request
    ) {
        CustomerVehicle updatedCustomerVehicle = customerVehiclePortIn.updateCustomerVehicle(
                customerVehicleId,
                customerVehicleApiMapper.toDomain(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle updated successfully",
                customerVehicleApiMapper.toAdminResponse(updatedCustomerVehicle)
        ));
    }

    @GetMapping("/{customerVehicleId}")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_READ_ALL', 'CUSTOMER_VEHICLE_READ_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> getCustomerVehicleById(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.getCustomerVehicleById(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer vehicle successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_READ_ALL', 'CUSTOMER_VEHICLE_READ_OWN')")
    public ResponseEntity<ApiResponse<List<CustomerVehicleAdminResponse>>> getAllCustomerVehicle(
            @ModelAttribute CustomerVehicleFilterRequest request
    ) {
        List<CustomerVehicle> customerVehicles = customerVehiclePortIn.getAllCustomerVehicle(
                request.customerId(),
                request.status(),
                request.vehicleTypeId(),
                request.isDefault(),
                request.keyword()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer vehicles successfully",
                customerVehicleApiMapper.toAdminResponses(customerVehicles)
        ));
    }

    @DeleteMapping("/{customerVehicleId}")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_DELETE_ALL', 'CUSTOMER_VEHICLE_DELETE_OWN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerVehicle(@PathVariable UUID customerVehicleId) {
        customerVehiclePortIn.deleteCustomerVehicle(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Customer vehicle inactivated successfully"));
    }

    @PatchMapping("/{customerVehicleId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> activateCustomerVehicle(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.activateCustomerVehicle(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle activated successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }

    @PatchMapping("/{customerVehicleId}/inactivate")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> inactivateCustomerVehicle(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.inactivateCustomerVehicle(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle inactivated successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }

    @PatchMapping("/{customerVehicleId}/block")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CUSTOMER_VEHICLE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> blockCustomerVehicle(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.blockCustomerVehicle(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle blocked successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }

    @PatchMapping("/{customerVehicleId}/mark-default")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> markCustomerVehicleAsDefault(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.markCustomerVehicleAsDefault(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle marked as default successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }

    @PatchMapping("/{customerVehicleId}/unmark-default")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerVehicleAdminResponse>> unmarkCustomerVehicleAsDefault(
            @PathVariable UUID customerVehicleId
    ) {
        CustomerVehicle customerVehicle = customerVehiclePortIn.unmarkCustomerVehicleAsDefault(customerVehicleId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer vehicle unmarked as default successfully",
                customerVehicleApiMapper.toAdminResponse(customerVehicle)
        ));
    }
}
