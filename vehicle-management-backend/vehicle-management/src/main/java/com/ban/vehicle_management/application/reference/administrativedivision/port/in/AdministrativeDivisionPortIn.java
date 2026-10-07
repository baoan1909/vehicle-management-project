package com.ban.vehicle_management.application.reference.administrativedivision.port.in;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivisionPath;
import java.util.List;

public interface AdministrativeDivisionPortIn {
    List<AdministrativeDivision> getCurrentProvinces();
    List<AdministrativeDivision> getCurrentWards(String provinceCode);
    AdministrativeDivisionPath getCurrentWardPath(String wardCode);
    List<AdministrativeDivision> getLegacyProvinces();
    List<AdministrativeDivision> getLegacyDistricts(String provinceCode);
    List<AdministrativeDivision> getLegacyWards(String districtCode);
    AdministrativeDivisionPath getLegacyWardPath(String wardCode);
}
