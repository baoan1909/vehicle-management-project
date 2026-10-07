package com.ban.vehicle_management.application.reference.administrativedivision.usecase;

import com.ban.vehicle_management.application.reference.administrativedivision.port.in.AdministrativeDivisionPortIn;
import com.ban.vehicle_management.application.reference.administrativedivision.port.out.AdministrativeDivisionQueryPortOut;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivisionPath;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdministrativeDivisionUseCaseImpl implements AdministrativeDivisionPortIn {

    private final AdministrativeDivisionQueryPortOut queryPort;

    public AdministrativeDivisionUseCaseImpl(AdministrativeDivisionQueryPortOut queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<AdministrativeDivision> getCurrentProvinces() {
        return queryPort.findCurrentProvinces();
    }

    @Override
    public List<AdministrativeDivision> getCurrentWards(String provinceCode) {
        if (!queryPort.existsCurrentProvince(provinceCode)) {
            throw new NotFoundException("Current province not found");
        }
        return queryPort.findCurrentWards(provinceCode);
    }

    @Override
    public AdministrativeDivisionPath getCurrentWardPath(String wardCode) {
        return queryPort.findCurrentWardPath(wardCode)
                .orElseThrow(() -> new NotFoundException("Current ward not found"));
    }

    @Override
    public List<AdministrativeDivision> getLegacyProvinces() {
        return queryPort.findLegacyProvinces();
    }

    @Override
    public List<AdministrativeDivision> getLegacyDistricts(String provinceCode) {
        if (!queryPort.existsLegacyProvince(provinceCode)) {
            throw new NotFoundException("Legacy province not found");
        }
        return queryPort.findLegacyDistricts(provinceCode);
    }

    @Override
    public List<AdministrativeDivision> getLegacyWards(String districtCode) {
        if (!queryPort.existsLegacyDistrict(districtCode)) {
            throw new NotFoundException("Legacy district not found");
        }
        return queryPort.findLegacyWards(districtCode);
    }

    @Override
    public AdministrativeDivisionPath getLegacyWardPath(String wardCode) {
        return queryPort.findLegacyWardPath(wardCode)
                .orElseThrow(() -> new NotFoundException("Legacy ward not found"));
    }
}