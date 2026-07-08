package com.ban.vehicle_management.domain.operations.chatmessage.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class ChatAttachmentPolicy {

    public static final int MAX_IMAGES_PER_MESSAGE = 5;

    public void validateImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("files must not be empty");
        }
        if (files.size() > MAX_IMAGES_PER_MESSAGE) {
            throw new BadRequestException("files must not contain more than " + MAX_IMAGES_PER_MESSAGE + " images");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("files must not contain empty images");
            }
        }
    }
}
