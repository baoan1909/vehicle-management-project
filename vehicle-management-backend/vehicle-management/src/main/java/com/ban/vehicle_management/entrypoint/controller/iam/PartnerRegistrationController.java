package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.partnerregistration.mapper.PartnerRegistrationApiMapper;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CreatePartnerRegistrationRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.ReviewPartnerRegistrationRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerRegistrationResponse;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
public class PartnerRegistrationController {
    private final PartnerRegistrationPortIn partnerRegistrationPortIn;
    private final PartnerRegistrationApiMapper partnerRegistrationApiMapper;
    public PartnerRegistrationController(PartnerRegistrationPortIn partnerRegistrationPortIn, PartnerRegistrationApiMapper partnerRegistrationApiMapper) { this.partnerRegistrationPortIn = partnerRegistrationPortIn; this.partnerRegistrationApiMapper = partnerRegistrationApiMapper; }

    @PostMapping("/api/public/partner-registrations")
    public ResponseEntity<ApiResponse<PartnerRegistrationResponse>> submit(@RequestBody CreatePartnerRegistrationRequest request) {
        PartnerRegistrationResult result = partnerRegistrationPortIn.submitRegistration(partnerRegistrationApiMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đã gửi yêu cầu trở thành đối tác. CoParking sẽ liên hệ với bạn sớm.", new PartnerRegistrationResponse(result.approvalRequestId(), result.status().name(), "PENDING")));
    }

    @GetMapping("/api/iam/partner-registrations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_READ_ALL')")
    public ResponseEntity<ApiResponse<List<PartnerRegistrationResult>>> list(@RequestParam(required = false) ApprovalRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched partner registrations successfully", partnerRegistrationPortIn.getPartnerRegistrations(status)));
    }

    @PostMapping("/api/iam/partner-registrations/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_CREATE_ALL')")
    public ResponseEntity<ApiResponse<PartnerRegistrationResult>> approve(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewPartnerRegistrationRequest request
    ) {
        PartnerRegistrationResult result = partnerRegistrationPortIn.approveRegistration(
                approvalRequestId,
                new ReviewPartnerRegistrationCommand(request == null ? null : request.note())
        );
        return ResponseEntity.ok(ApiResponse.ok("Partner registration approved successfully", result));
    }

    @PostMapping("/api/iam/partner-registrations/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_CREATE_ALL')")
    public ResponseEntity<ApiResponse<PartnerRegistrationResult>> reject(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewPartnerRegistrationRequest request
    ) {
        PartnerRegistrationResult result = partnerRegistrationPortIn.rejectRegistration(
                approvalRequestId,
                new ReviewPartnerRegistrationCommand(request == null ? null : request.note())
        );
        return ResponseEntity.ok(ApiResponse.ok("Partner registration rejected successfully", result));
    }
}
