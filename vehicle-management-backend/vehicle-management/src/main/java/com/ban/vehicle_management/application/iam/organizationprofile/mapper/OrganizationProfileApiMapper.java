package com.ban.vehicle_management.application.iam.organizationprofile.mapper;

import com.ban.vehicle_management.application.iam.organizationprofile.model.command.RequestOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.ReviewOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileResult;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.RequestOrganizationProfileUpdateRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.ReviewOrganizationProfileUpdateRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationProfileResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationProfileUpdateRequestResponse;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationProfileApiMapper {

    @Mapping(target = "structuredAddress", source = "structuredAddress")
    RequestOrganizationProfileUpdateCommand toCommand(RequestOrganizationProfileUpdateRequest request);

    ReviewOrganizationProfileUpdateCommand toCommand(ReviewOrganizationProfileUpdateRequest request);

    @Mapping(target = "organizationId", source = "organization.organizationId")
    @Mapping(target = "code", source = "organization.code")
    @Mapping(target = "name", source = "organization.name")
    @Mapping(target = "addressDetail", source = "organization.addressDetail")
    @Mapping(target = "provinceCode", source = "organization.provinceCode")
    @Mapping(target = "districtCode", source = "organization.districtCode")
    @Mapping(target = "wardCode", source = "organization.wardCode")
    @Mapping(target = "addressDisplay", source = "organization.addressDisplay")
    @Mapping(target = "status", source = "organization.status")
    @Mapping(target = "createdAt", source = "organization.createdAt")
    @Mapping(target = "updatedAt", source = "organization.updatedAt")
    @Mapping(target = "representativeUsername", source = "representativeUsername")
    @Mapping(target = "representativeEmail", source = "representativeEmail")
    @Mapping(target = "totalParkingLots", source = "operationalSummary.totalParkingLots")
    @Mapping(target = "activeParkingLots", source = "operationalSummary.activeParkingLots")
    @Mapping(target = "setupParkingLots", source = "operationalSummary.setupParkingLots")
    @Mapping(target = "maintenanceParkingLots", source = "operationalSummary.maintenanceParkingLots")
    @Mapping(target = "latestUpdateRequest", source = "latestUpdateRequest")
    OrganizationProfileResponse toResponse(OrganizationProfileResult result);

    OrganizationProfileUpdateRequestResponse toResponse(OrganizationProfileUpdateRequestResult result);

    default VietnamAddress toDomain(VietnamAddressRequest request) {
        return request == null ? null : new VietnamAddress(
                request.provinceCode(), request.districtCode(), request.wardCode(), request.addressDetail(), null
        );
    }
}
