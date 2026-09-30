package com.ban.vehicle_management.application.reference.administrativedivision.port.in;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import java.util.List;

public interface AdministrativeDivisionPortIn {
    List<AdministrativeDivision> getCurrentProvinces();
    List<AdministrativeDivision> getCurrentWards(String provinceCode);
    List<AdministrativeDivision> getLegacyProvinces();
    List<AdministrativeDivision> getLegacyDistricts(String provinceCode);
    List<AdministrativeDivision> getLegacyWards(String districtCode);
}
