package com.ban.vehicle_management.entrypoint.converter;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartFileBytesConverter {

    public byte[] read(MultipartFile file, String errorMessage) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException(errorMessage);
        }
    }

    public byte[] read(
            MultipartFile file,
            long maxSize,
            String sizeErrorMessage,
            String readErrorMessage) {
        if (file.getSize() > maxSize) {
            throw new BadRequestException(sizeErrorMessage);
        }
        return read(file, readErrorMessage);
    }
}
