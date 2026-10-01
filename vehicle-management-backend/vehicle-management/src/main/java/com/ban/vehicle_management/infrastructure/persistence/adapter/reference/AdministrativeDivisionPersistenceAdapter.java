package com.ban.vehicle_management.infrastructure.persistence.adapter.reference;

import com.ban.vehicle_management.application.reference.administrativedivision.port.out.AdministrativeDivisionQueryPortOut;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyDistrictEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyProvinceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyWardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.ProvinceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.WardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyDistrictRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyProvinceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.LegacyWardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.ProvinceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.WardRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdministrativeDivisionPersistenceAdapter implements AdministrativeDivisionQueryPortOut {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final LegacyProvinceRepository legacyProvinceRepository;
    private final LegacyDistrictRepository legacyDistrictRepository;
    private final LegacyWardRepository legacyWardRepository;

    public AdministrativeDivisionPersistenceAdapter(
            ProvinceRepository provinceRepository,
            WardRepository wardRepository,
            LegacyProvinceRepository legacyProvinceRepository,
            LegacyDistrictRepository legacyDistrictRepository,
            LegacyWardRepository legacyWardRepository
    ) {
        this.provinceRepository = provinceRepository;
        this.wardRepository = wardRepository;
        this.legacyProvinceRepository = legacyProvinceRepository;
        this.legacyDistrictRepository = legacyDistrictRepository;
        this.legacyWardRepository = legacyWardRepository;
    }

    @Override
    public List<AdministrativeDivision> findCurrentProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsCurrentProvince(String provinceCode) {
        return provinceRepository.existsById(provinceCode);
    }

    @Override
    public List<AdministrativeDivision> findCurrentWards(String provinceCode) {
        return wardRepository.findAllByProvinceCodeOrderByNameAsc(provinceCode).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AdministrativeDivision> findLegacyProvinces() {
        return legacyProvinceRepository.findAllByOrderByNameAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsLegacyProvince(String provinceCode) {
        return legacyProvinceRepository.existsById(provinceCode);
    }

    @Override
    public List<AdministrativeDivision> findLegacyDistricts(String provinceCode) {
        return legacyDistrictRepository.findAllByProvinceCodeOrderByNameAsc(provinceCode).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsLegacyDistrict(String districtCode) {
        return legacyDistrictRepository.existsById(districtCode);
    }

    @Override
    public List<AdministrativeDivision> findLegacyWards(String districtCode) {
        return legacyWardRepository.findAllByDistrictCodeOrderByNameAsc(districtCode).stream().map(this::toDomain).toList();
    }

    private AdministrativeDivision toDomain(ProvinceEntity entity) {
        return new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName());
    }

    private AdministrativeDivision toDomain(WardEntity entity) {
        return new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName());
    }

    private AdministrativeDivision toDomain(LegacyProvinceEntity entity) {
        return new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName());
    }

    private AdministrativeDivision toDomain(LegacyDistrictEntity entity) {
        return new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName());
    }

    private AdministrativeDivision toDomain(LegacyWardEntity entity) {
        return new AdministrativeDivision(entity.getCode(), entity.getName(), entity.getFullName());
    }
}
