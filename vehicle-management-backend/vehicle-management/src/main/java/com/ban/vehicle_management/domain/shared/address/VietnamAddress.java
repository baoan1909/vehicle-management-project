package com.ban.vehicle_management.domain.shared.address;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VietnamAddress {

    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String addressDetail;
    private String addressDisplay;

    public static VietnamAddress ofCurrent(
            String provinceCode,
            String wardCode,
            String addressDetail
    ) {
        VietnamAddress address = new VietnamAddress();
        address.setProvinceCode(provinceCode);
        address.setDistrictCode(null);
        address.setWardCode(wardCode);
        address.setAddressDetail(addressDetail);
        return address;
    }

    public static VietnamAddress ofLegacy(
            String provinceCode,
            String districtCode,
            String wardCode,
            String addressDetail
    ) {
        VietnamAddress address = new VietnamAddress();
        address.setProvinceCode(provinceCode);
        address.setDistrictCode(districtCode);
        address.setWardCode(wardCode);
        address.setAddressDetail(addressDetail);
        return address;
    }

    public boolean isCurrent() {
        return districtCode == null || districtCode.isBlank();
    }

    public boolean isLegacy() {
        return districtCode != null && !districtCode.isBlank();
    }

    public void validateStructure() {
        if (provinceCode == null || provinceCode.isBlank()) {
            throw new BadRequestException("provinceCode must not be blank");
        }
        if (wardCode == null || wardCode.isBlank()) {
            throw new BadRequestException("wardCode must not be blank");
        }
        if (addressDetail == null || addressDetail.isBlank()) {
            throw new BadRequestException("addressDetail must not be blank");
        }
    }

    public void normalize() {
        this.provinceCode = TextValidationUtils.normalizeRequiredText(provinceCode, "provinceCode", 20);
        this.districtCode = TextValidationUtils.normalizeNullableText(districtCode, "districtCode", 20);
        this.wardCode = TextValidationUtils.normalizeRequiredText(wardCode, "wardCode", 20);
        this.addressDetail = TextValidationUtils.normalizeRequiredText(addressDetail, "addressDetail", 255);
    }

    public void setAddressDisplay(String addressDisplay) {
        this.addressDisplay = addressDisplay;
    }
}
