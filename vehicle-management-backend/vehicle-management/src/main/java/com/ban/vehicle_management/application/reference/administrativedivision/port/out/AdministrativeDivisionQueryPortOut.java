package com.ban.vehicle_management.application.reference.administrativedivision.port.out;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import java.util.List;

public interface AdministrativeDivisionQueryPortOut {
    List<AdministrativeDivision> findCurrentProvinces();
    boolean existsCurrentProvince(String provinceCode);
    List<AdministrativeDivision> findCurrentWards(String provinceCode);
    List<AdministrativeDivision> findLegacyProvinces();
    boolean existsLegacyProvince(String provinceCode);
    List<AdministrativeDivision> findLegacyDistricts(String provinceCode);
    boolean existsLegacyDistrict(String districtCode);
    List<AdministrativeDivision> findLegacyWards(String districtCode);
}
