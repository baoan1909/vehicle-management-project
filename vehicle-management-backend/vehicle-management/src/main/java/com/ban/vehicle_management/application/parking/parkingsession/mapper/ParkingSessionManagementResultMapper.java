package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.storage.config.StorageAccessTimeProperties;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ParkingSessionManagementResultMapper {

    private StorageAccessTimeProperties storageAccessTimeProperties = new StorageAccessTimeProperties();

    @Autowired
    void configureStorageAccessTime(StorageAccessTimeProperties properties) {
        this.storageAccessTimeProperties = properties;
    }

    public abstract List<ParkingSessionManagementResult> withResolvedEventImageUrls(
            List<ParkingSessionManagementResult> sessions,
            @Context FileAccessPort fileAccessPort
    );

    @Mapping(target = "events", source = "events", qualifiedByName = "resolveEvents")
    public abstract ParkingSessionManagementResult withResolvedEventImageUrls(
            ParkingSessionManagementResult session,
            @Context FileAccessPort fileAccessPort
    );

    @Named("resolveEvents")
    @IterableMapping(qualifiedByName = "resolveEvent")
    abstract List<ParkingSessionManagementResult.EventResult> resolveEvents(
            List<ParkingSessionManagementResult.EventResult> events,
            @Context FileAccessPort fileAccessPort
    );

    @Named("resolveEvent")
    @Mapping(target = "licensePlateImagePath", source = "licensePlateImagePath", qualifiedByName = "resolvePrivateReadUrl")
    @Mapping(target = "personImagePath", source = "personImagePath", qualifiedByName = "resolvePrivateReadUrl")
    abstract ParkingSessionManagementResult.EventResult resolveEvent(
            ParkingSessionManagementResult.EventResult event,
            @Context FileAccessPort fileAccessPort
    );

    @Named("resolvePrivateReadUrl")
    String resolvePrivateReadUrl(String objectKey, @Context FileAccessPort fileAccessPort) {
        if (objectKey == null || objectKey.isBlank() || isBrowserReachableUrl(objectKey)) {
            return objectKey;
        }
        return fileAccessPort.createReadUrl(
                objectKey,
                storageAccessTimeProperties.parkingImageReadUrlExpirySeconds()
        );
    }

    boolean isBrowserReachableUrl(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/");
    }
}
