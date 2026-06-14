package com.ban.vehicle_management.infrastructure.storage;

import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StorageObjectKeyGenerator {

    private static final int MAX_OBJECT_KEY_LENGTH = 255;

    public String generate(StoreFileCommand command, String extension) {
        if (command == null) {
            throw new BadRequestException("storeFileCommand must not be null");
        }
        if (command.bucket() == null) {
            throw new BadRequestException("storage bucket must not be null");
        }
        if (command.folder() == null) {
            throw new BadRequestException("storage folder must not be null");
        }
        if (command.resourceId() == null) {
            throw new BadRequestException("resourceId must not be null");
        }

        String normalizedExtension = normalizeExtension(extension);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String objectKey = "%s/%04d/%02d/%02d/%s/%s-%s-%s.%s".formatted(
                command.folder().pathSegment(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                command.resourceId(),
                command.bucket().objectKeyPrefix(),
                UUID.randomUUID(),
                command.folder().fileRole(),
                normalizedExtension
        );

        if (objectKey.length() > MAX_OBJECT_KEY_LENGTH) {
            throw new BadRequestException("Generated object key must not exceed 255 characters");
        }
        return objectKey;
    }

    private String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new BadRequestException("file extension must not be blank");
        }
        return extension.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
    }
}
