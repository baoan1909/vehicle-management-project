package com.ban.vehicle_management.domain.people.userprofile.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends AuditableDomainModel {

    private UUID userProfileId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String addressDetail;
    private String provinceCode;
    private String wardCode;
    private String districtCode;
    private String addressDisplay;
    private String identifyCard;
    private String avatarUrl;
    private UserProfileStatus status;

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
}

