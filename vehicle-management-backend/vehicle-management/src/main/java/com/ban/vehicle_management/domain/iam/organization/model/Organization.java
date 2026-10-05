package com.ban.vehicle_management.domain.iam.organization.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Organization extends AuditableDomainModel {

    private UUID organizationId;
    private String code;
    private String name;
    private String address;
    private String addressDetail;
    private String provinceCode;
    private String wardCode;
    private String districtCode;
    private String addressDisplay;
    private OrganizationStatus status;

    public VietnamAddress getStructuredAddress() {
        if (addressDetail == null && provinceCode == null && wardCode == null) {
            return null;
        }
        VietnamAddress address = new VietnamAddress();
        address.setAddressDetail(addressDetail);
        address.setProvinceCode(provinceCode);
        address.setWardCode(wardCode);
        address.setDistrictCode(districtCode);
        address.setAddressDisplay(addressDisplay);
        return address;
    }

    public void setStructuredAddress(VietnamAddress address) {
        if (address == null) {
            this.addressDetail = null;
            this.provinceCode = null;
            this.wardCode = null;
            this.districtCode = null;
            this.addressDisplay = null;
            return;
        }
        this.addressDetail = address.getAddressDetail();
        this.provinceCode = address.getProvinceCode();
        this.wardCode = address.getWardCode();
        this.districtCode = address.getDistrictCode();
        this.addressDisplay = address.getAddressDisplay();
        this.address = address.getAddressDisplay();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrganizationAddress {
        private VietnamAddress structuredAddress;
    }
}
