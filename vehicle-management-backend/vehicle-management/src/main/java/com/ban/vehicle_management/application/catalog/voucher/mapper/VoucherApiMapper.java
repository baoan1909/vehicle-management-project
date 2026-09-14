package com.ban.vehicle_management.application.catalog.voucher.mapper;

import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.request.CreateVoucherRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.request.UpdateVoucherRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response.VoucherAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response.VoucherCustomerBannerResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherApiMapper {
    @Mapping(target = "voucherId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Voucher toDomain(CreateVoucherRequest request);

    @Mapping(target = "voucherId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Voucher toDomain(UpdateVoucherRequest request);

    VoucherAdminResponse toAdminResponse(Voucher voucher);
    List<VoucherAdminResponse> toAdminResponses(List<Voucher> vouchers);
    VoucherCustomerBannerResponse toCustomerBannerResponse(Voucher voucher);
    List<VoucherCustomerBannerResponse> toCustomerBannerResponses(List<Voucher> vouchers);

    default String map(Instant value) { return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE); }
}
