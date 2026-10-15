package com.ban.vehicle_management.application.billing.platformfee.mapper;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeTypeDetailResult;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.CreatePlatformFeeTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.UpdatePlatformFeeTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeePageResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeePreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeTypeAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeTypeDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeVersionAdminResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlatformFeeApiMapper {

    @Mapping(target = "platformFeeTypeId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "archivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stackable", source = "stackable")
    PlatformFeeType toDomain(CreatePlatformFeeTypeRequest request);

    @Mapping(target = "platformFeeTypeId", ignore = true)
    @Mapping(target = "feeCode", ignore = true)
    @Mapping(target = "transactionType", ignore = true)
    @Mapping(target = "calculationType", ignore = true)
    @Mapping(target = "chargedTo", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "archivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "stackable", source = "stackable")
    PlatformFeeType toDraftChanges(UpdatePlatformFeeTypeRequest request);

    PlatformFeeTypeAdminResponse toAdminResponse(PlatformFeeType domain);

    PlatformFeeVersionAdminResponse toVersionResponse(PlatformFeeRuleVersion domain);

    java.util.List<PlatformFeeVersionAdminResponse> toVersionResponses(
            java.util.List<PlatformFeeRuleVersion> domains);

    default PlatformFeePageResponse toPageResponse(PlatformFeePageResult result) {
        return new PlatformFeePageResponse(
                result.items().stream().map(this::toAdminResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    default PlatformFeeTypeDetailResponse toDetailResponse(PlatformFeeTypeDetailResult result) {
        return new PlatformFeeTypeDetailResponse(
                toAdminResponse(result.feeType()),
                result.effectiveVersion() == null ? null : toVersionResponse(result.effectiveVersion()),
                result.upcomingVersion() == null ? null : toVersionResponse(result.upcomingVersion()),
                result.totalVersions(),
                toVersionResponses(result.recentVersions()));
    }

    default PlatformFeePreviewResponse toPreviewResponse(PlatformFeePreviewResult result) {
        return new PlatformFeePreviewResponse(
                result.transactionType(),
                result.effectiveAt(),
                result.grossAmount(),
                result.customerPaidAmount(),
                result.feeLines().stream()
                        .map(line -> new PlatformFeePreviewResponse.PlatformFeePreviewLineResponse(
                                line.feeTypeId(),
                                line.ruleVersionId(),
                                line.feeCode(),
                                line.feeName(),
                                line.version(),
                                line.feeRate(),
                                line.feeBasis(),
                                line.chargedTo(),
                                line.basisAmount(),
                                line.feeAmount()))
                        .toList(),
                result.totalFeeAmount(),
                result.effectiveTotalRate(),
                result.partnerRemainingAmount());
    }

    default boolean mapStackable(Boolean stackable) {
        return stackable == null || stackable;
    }
}
