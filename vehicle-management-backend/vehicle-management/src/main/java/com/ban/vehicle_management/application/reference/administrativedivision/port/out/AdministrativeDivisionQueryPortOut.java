package com.ban.vehicle_management.application.reference.administrativedivision.port.out;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivisionPath;
import java.util.List;
import java.util.Optional;

public interface AdministrativeDivisionQueryPortOut {
    List<AdministrativeDivision> findCurrentProvinces();
    boolean existsCurrentProvince(String provinceCode);
    List<AdministrativeDivision> findCurrentWards(String provinceCode);
    Optional<AdministrativeDivisionPath> findCurrentWardPath(String wardCode);
    List<AdministrativeDivision> findLegacyProvinces();
    boolean existsLegacyProvince(String provinceCode);
    List<AdministrativeDivision> findLegacyDistricts(String provinceCode);
    boolean existsLegacyDistrict(String districtCode);
    List<AdministrativeDivision> findLegacyWards(String districtCode);
    Optional<AdministrativeDivisionPath> findLegacyWardPath(String wardCode);
}
