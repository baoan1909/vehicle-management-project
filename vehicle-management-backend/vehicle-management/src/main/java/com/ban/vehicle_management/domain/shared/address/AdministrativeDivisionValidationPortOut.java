package com.ban.vehicle_management.domain.shared.address;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import java.util.List;
import java.util.Optional;

public interface AdministrativeDivisionValidationPortOut {

    List<AdministrativeDivision> findCurrentProvinces();

    Optional<AdministrativeDivision> findCurrentProvinceByCode(String provinceCode);

    List<AdministrativeDivision> findCurrentWards(String provinceCode);

    Optional<AdministrativeDivision> findCurrentWardByCode(String wardCode);

    boolean existsCurrentWardInProvince(String wardCode, String provinceCode);

    List<AdministrativeDivision> findLegacyProvinces();

    Optional<AdministrativeDivision> findLegacyProvinceByCode(String provinceCode);

    List<AdministrativeDivision> findLegacyDistricts(String provinceCode);

    Optional<AdministrativeDivision> findLegacyDistrictByCode(String districtCode);

    List<AdministrativeDivision> findLegacyWards(String districtCode);

    Optional<AdministrativeDivision> findLegacyWardByCode(String wardCode);

    boolean existsLegacyWardInDistrict(String wardCode, String districtCode);

    boolean existsLegacyDistrictInProvince(String districtCode, String provinceCode);
}