package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.voucher.mapper.VoucherApiMapper;
import com.ban.vehicle_management.application.catalog.voucher.port.in.VoucherPortIn;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.request.CreateVoucherRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.request.UpdateVoucherRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response.VoucherAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response.VoucherCustomerBannerResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/vouchers")
public class VoucherController {
    private final VoucherPortIn voucherPortIn;
    private final VoucherApiMapper voucherApiMapper;

    public VoucherController(VoucherPortIn voucherPortIn, VoucherApiMapper voucherApiMapper) {
        this.voucherPortIn = voucherPortIn;
        this.voucherApiMapper = voucherApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_CREATE_ALL')")
    public ResponseEntity<ApiResponse<VoucherAdminResponse>> createVoucher(@RequestBody CreateVoucherRequest request) {
        Voucher voucher = voucherPortIn.createVoucher(voucherApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Voucher created successfully", voucherApiMapper.toAdminResponse(voucher)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_READ_ALL')")
    public ResponseEntity<ApiResponse<List<VoucherAdminResponse>>> getVouchers() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched vouchers successfully", voucherApiMapper.toAdminResponses(voucherPortIn.getVouchers())));
    }

    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<VoucherCustomerBannerResponse>>> getCustomerVoucherBanners() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer voucher banners successfully",
                voucherApiMapper.toCustomerBannerResponses(voucherPortIn.getCustomerVisibleVouchers(Instant.now()))
        ));
    }

    @GetMapping("/{voucherId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_READ_ALL')")
    public ResponseEntity<ApiResponse<VoucherAdminResponse>> getVoucher(@PathVariable UUID voucherId) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched voucher successfully", voucherApiMapper.toAdminResponse(voucherPortIn.getVoucherById(voucherId))));
    }

    @PutMapping("/{voucherId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<VoucherAdminResponse>> updateVoucher(@PathVariable UUID voucherId, @RequestBody UpdateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Voucher updated successfully", voucherApiMapper.toAdminResponse(voucherPortIn.updateVoucher(voucherId, voucherApiMapper.toDomain(request)))));
    }

    @PatchMapping("/{voucherId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<VoucherAdminResponse>> activateVoucher(@PathVariable UUID voucherId) {
        return ResponseEntity.ok(ApiResponse.ok("Voucher activated successfully", voucherApiMapper.toAdminResponse(voucherPortIn.activateVoucher(voucherId))));
    }

    @PatchMapping("/{voucherId}/pause")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VOUCHER_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<VoucherAdminResponse>> pauseVoucher(@PathVariable UUID voucherId) {
        return ResponseEntity.ok(ApiResponse.ok("Voucher paused successfully", voucherApiMapper.toAdminResponse(voucherPortIn.pauseVoucher(voucherId))));
    }
}
