package com.ban.vehicle_management.entrypoint.converter;

import com.ban.vehicle_management.application.ai.command.UploadKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.mapper.KnowledgeDocumentApiMapper;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class KnowledgeDocumentUploadConverter {

    private final KnowledgeDocumentApiMapper mapper;
    private final MultipartFileBytesConverter fileBytesConverter;

    public KnowledgeDocumentUploadConverter(
            KnowledgeDocumentApiMapper mapper,
            MultipartFileBytesConverter fileBytesConverter) {
        this.mapper = mapper;
        this.fileBytesConverter = fileBytesConverter;
    }

    public UploadKnowledgeDocumentCommand convert(
            UUID sourceId,
            String title,
            String requestIdempotencyKey,
            String headerIdempotencyKey,
            MultipartFile file) {
        String idempotencyKey = requestIdempotencyKey != null
                ? requestIdempotencyKey
                : headerIdempotencyKey;
        return mapper.toUploadCommand(
                sourceId,
                title,
                file.getOriginalFilename(),
                file.getContentType(),
                fileBytesConverter.read(
                        file,
                        KnowledgeDocumentFilePolicy.DEFAULT_MAX_FILE_SIZE_BYTES,
                        "Tệp tải lên vượt quá dung lượng cho phép",
                        "Không thể đọc tệp tải lên"),
                idempotencyKey);
    }
}
