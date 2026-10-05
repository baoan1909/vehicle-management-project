package com.ban.vehicle_management.infrastructure.reference.administrativedivision.adapter;

import com.ban.vehicle_management.application.reference.administrativedivision.port.out.AdministrativeDivisionQueryPortOut;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.domain.shared.address.AdministrativeDivisionValidationPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyDistrictRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyProvinceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyWardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.ProvinceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.WardRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AdministrativeDivisionValidationAdapter implements AdministrativeDivisionValidationPortOut {

    private final AdministrativeDivisionQueryPortOut queryPort;
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final LegacyProvinceRepository legacyProvinceRepository;
    private final LegacyDistrictRepository legacyDistrictRepository;
    private final LegacyWardRepository legacyWardRepository;

    public AdministrativeDivisionValidationAdapter(
            AdministrativeDivisionQueryPortOut queryPort,
            ProvinceRepository provinceRepository,
            WardRepository wardRepository,
            LegacyProvinceRepository legacyProvinceRepository,
            LegacyDistrictRepository legacyDistrictRepository,
            LegacyWardRepository legacyWardRepository
    ) {
        this.queryPort = queryPort;
        this.provinceRepository = provinceRepository;
        this.wardRepository = wardRepository;
        this.legacyProvinceRepository = legacyProvinceRepository;
        this.legacyDistrictRepository = legacyDistrictRepository;
        this.legacyWardRepository = legacyWardRepository;
    }

    @Override
    public List<AdministrativeDivision> findCurrentProvinces() {
        return queryPort.findCurrentProvinces();
    }

    @Override
    public Optional<AdministrativeDivision> findCurrentProvinceByCode(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            return Optional.empty();
        }
        return provinceRepository.findById(provinceCode)
                .map(entity -> new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName()));
    }

    @Override
    public List<AdministrativeDivision> findCurrentWards(String provinceCode) {
        return queryPort.findCurrentWards(provinceCode);
    }

    @Override
    public Optional<AdministrativeDivision> findCurrentWardByCode(String wardCode) {
        if (wardCode == null || wardCode.isBlank()) {
            return Optional.empty();
        }
        return wardRepository.findById(wardCode)
                .map(entity -> new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName()));
    }

    @Override
    public boolean existsCurrentWardInProvince(String wardCode, String provinceCode) {
        if (wardCode == null || wardCode.isBlank() || provinceCode == null || provinceCode.isBlank()) {
            return false;
        }
        return wardRepository.findById(wardCode)
                .map(ward -> provinceCode.equals(ward.getProvinceCode()))
                .orElse(false);
    }

    @Override
    public List<AdministrativeDivision> findLegacyProvinces() {
        return queryPort.findLegacyProvinces();
    }

    @Override
    public Optional<AdministrativeDivision> findLegacyProvinceByCode(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            return Optional.empty();
        }
        return legacyProvinceRepository.findById(provinceCode)
                .map(entity -> new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName()));
    }

    @Override
    public List<AdministrativeDivision> findLegacyDistricts(String provinceCode) {
        return queryPort.findLegacyDistricts(provinceCode);
    }

    @Override
    public Optional<AdministrativeDivision> findLegacyDistrictByCode(String districtCode) {
        if (districtCode == null || districtCode.isBlank()) {
            return Optional.empty();
        }
        return legacyDistrictRepository.findById(districtCode)
                .map(entity -> new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName()));
    }

    @Override
    public List<AdministrativeDivision> findLegacyWards(String districtCode) {
        return queryPort.findLegacyWards(districtCode);
    }

    @Override
    public Optional<AdministrativeDivision> findLegacyWardByCode(String wardCode) {
        if (wardCode == null || wardCode.isBlank()) {
            return Optional.empty();
        }
        return legacyWardRepository.findById(wardCode)
                .map(entity -> new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName()));
    }

    @Override
    public boolean existsLegacyWardInDistrict(String wardCode, String districtCode) {
        if (wardCode == null || wardCode.isBlank() || districtCode == null || districtCode.isBlank()) {
            return false;
        }
        return legacyWardRepository.findById(wardCode)
                .map(ward -> districtCode.equals(ward.getDistrictCode()))
                .orElse(false);
    }

    @Override
    public boolean existsLegacyDistrictInProvince(String districtCode, String provinceCode) {
        if (districtCode == null || districtCode.isBlank() || provinceCode == null || provinceCode.isBlank()) {
            return false;
        }
        return legacyDistrictRepository.findById(districtCode)
                .map(district -> provinceCode.equals(district.getProvinceCode()))
                .orElse(false);
    }
}
