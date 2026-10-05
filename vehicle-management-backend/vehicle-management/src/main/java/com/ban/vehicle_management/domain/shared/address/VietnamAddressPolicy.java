package com.ban.vehicle_management.domain.shared.address;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VietnamAddressPolicy {

    private final AdministrativeDivisionValidationPortOut validationPort;

    public VietnamAddressPolicy(AdministrativeDivisionValidationPortOut validationPort) {
        this.validationPort = validationPort;
    }

    public void validateAndBuildDisplay(VietnamAddress address) {
        address.validateStructure();
        address.normalize();

        if (address.isCurrent()) {
            validateCurrentAddress(address);
        } else {
            validateLegacyAddress(address);
        }
        address.setAddressDisplay(buildDisplayAddress(address));
    }

    private void validateCurrentAddress(VietnamAddress address) {
        Optional<AdministrativeDivision> province = validationPort.findCurrentProvinceByCode(address.getProvinceCode());
        if (province.isEmpty()) {
            throw new BadRequestException("Current province not found: " + address.getProvinceCode());
        }

        Optional<AdministrativeDivision> ward = validationPort.findCurrentWardByCode(address.getWardCode());
        if (ward.isEmpty()) {
            throw new BadRequestException("Current ward not found: " + address.getWardCode());
        }

        if (!validationPort.existsCurrentWardInProvince(address.getWardCode(), address.getProvinceCode())) {
            throw new BadRequestException("Current ward does not belong to province: " + address.getWardCode() + " not in " + address.getProvinceCode());
        }
    }

    private void validateLegacyAddress(VietnamAddress address) {
        Optional<AdministrativeDivision> province = validationPort.findLegacyProvinceByCode(address.getProvinceCode());
        if (province.isEmpty()) {
            throw new BadRequestException("Legacy province not found: " + address.getProvinceCode());
        }

        Optional<AdministrativeDivision> district = validationPort.findLegacyDistrictByCode(address.getDistrictCode());
        if (district.isEmpty()) {
            throw new BadRequestException("Legacy district not found: " + address.getDistrictCode());
        }

        if (!validationPort.existsLegacyDistrictInProvince(address.getDistrictCode(), address.getProvinceCode())) {
            throw new BadRequestException("Legacy district does not belong to province: " + address.getDistrictCode() + " not in " + address.getProvinceCode());
        }

        Optional<AdministrativeDivision> ward = validationPort.findLegacyWardByCode(address.getWardCode());
        if (ward.isEmpty()) {
            throw new BadRequestException("Legacy ward not found: " + address.getWardCode());
        }

        if (!validationPort.existsLegacyWardInDistrict(address.getWardCode(), address.getDistrictCode())) {
            throw new BadRequestException("Legacy ward does not belong to district: " + address.getWardCode() + " not in " + address.getDistrictCode());
        }
    }

    private String buildDisplayAddress(VietnamAddress address) {
        if (address.isCurrent()) {
            Optional<AdministrativeDivision> ward = validationPort.findCurrentWardByCode(address.getWardCode());
            Optional<AdministrativeDivision> province = validationPort.findCurrentProvinceByCode(address.getProvinceCode());
            return String.join(", ",
                    TextValidationUtils.normalizeNullableText(address.getAddressDetail(), "addressDetail", 255),
                    ward.map(AdministrativeDivision::getFullName).orElse(""),
                    province.map(AdministrativeDivision::getFullName).orElse("")
            ).replaceAll(",,", ",").replaceAll("^,\\s*", "").replaceAll(",\\s*$", "");
        } else {
            Optional<AdministrativeDivision> ward = validationPort.findLegacyWardByCode(address.getWardCode());
            Optional<AdministrativeDivision> district = validationPort.findLegacyDistrictByCode(address.getDistrictCode());
            Optional<AdministrativeDivision> province = validationPort.findLegacyProvinceByCode(address.getProvinceCode());
            return String.join(", ",
                    TextValidationUtils.normalizeNullableText(address.getAddressDetail(), "addressDetail", 255),
                    ward.map(AdministrativeDivision::getFullName).orElse(""),
                    district.map(AdministrativeDivision::getFullName).orElse(""),
                    province.map(AdministrativeDivision::getFullName).orElse("")
            ).replaceAll(",,", ",").replaceAll("^,\\s*", "").replaceAll(",\\s*$", "");
        }
    }
}