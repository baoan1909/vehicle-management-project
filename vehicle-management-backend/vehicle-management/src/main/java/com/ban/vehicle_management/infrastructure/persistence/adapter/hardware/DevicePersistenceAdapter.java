package com.ban.vehicle_management.infrastructure.persistence.adapter.hardware;

import com.ban.vehicle_management.application.hardware.device.port.out.DevicePortOut;
import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.infrastructure.mapper.hardware.DevicePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.hardware.DeviceSpecifications;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DevicePersistenceAdapter implements DevicePortOut {

    private final DeviceRepository deviceRepository;
    private final LaneRepository laneRepository;
    private final DevicePersistenceMapper devicePersistenceMapper;

    public DevicePersistenceAdapter(
            DeviceRepository deviceRepository,
            LaneRepository laneRepository,
            DevicePersistenceMapper devicePersistenceMapper
    ) {
        this.deviceRepository = deviceRepository;
        this.laneRepository = laneRepository;
        this.devicePersistenceMapper = devicePersistenceMapper;
    }

    @Override
    public Device save(Device device) {
        DeviceEntity savedEntity = deviceRepository.saveAndFlush(
                devicePersistenceMapper.toEntity(device)
        );
        return devicePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Device> findById(UUID deviceId) {
        return deviceRepository.findById(deviceId)
                .map(devicePersistenceMapper::toDomain);
    }

    @Override
    public List<Device> findAll(
            UUID parkingLotId,
            UUID laneId,
            DeviceType deviceType,
            DeviceStatus status,
            String keyword
    ) {
        return deviceRepository.findAll(
                        DeviceSpecifications.withFilters(
                                parkingLotId,
                                laneId,
                                deviceType,
                                status,
                                keyword
                        )
                )
                .stream()
                .map(devicePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByDeviceCode(String deviceCode) {
        return deviceRepository.existsByDeviceCode(deviceCode);
    }

    @Override
    public boolean existsByDeviceCodeAndDeviceIdNot(
            String deviceCode,
            UUID deviceId
    ) {
        return deviceRepository.existsByDeviceCodeAndDeviceIdNot(
                deviceCode,
                deviceId
        );
    }

    @Override
    public boolean existsLaneInParkingLot(UUID laneId, UUID parkingLotId) {
        return laneRepository.existsLaneInParkingLot(laneId, parkingLotId);
    }

    @Override
    public boolean existsNonClosedLaneById(UUID laneId) {
        return laneRepository.existsByLaneIdAndStatusNot(
                laneId,
                LaneStatus.CLOSED
        );
    }
}