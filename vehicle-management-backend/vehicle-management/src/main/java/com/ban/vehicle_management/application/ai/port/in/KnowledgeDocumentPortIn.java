package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.command.ArchiveKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.KnowledgeDocumentUploadResult;
import com.ban.vehicle_management.application.ai.command.PublishKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.ReindexKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.RejectKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.UploadKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeDocumentPortIn {

    KnowledgeDocumentUploadResult uploadDocument(UploadKnowledgeDocumentCommand command);

    KnowledgeDocument publishDocument(PublishKnowledgeDocumentCommand command);

    KnowledgeDocument rejectDocument(RejectKnowledgeDocumentCommand command);

    KnowledgeDocument archiveDocument(ArchiveKnowledgeDocumentCommand command);

    KnowledgeDocument reindexDocument(ReindexKnowledgeDocumentCommand command);

    KnowledgeDocument documentDetail(UUID documentId);

    List<KnowledgeDocument> listDocuments();

    Page<KnowledgeDocument> listDocuments(KnowledgeDocumentQuery query, Pageable pageable);

    List<KnowledgeDocumentBlock> documentBlocks(UUID documentId, int documentVersion);

    List<KnowledgeChunkSnapshot> documentChunks(UUID documentId, int documentVersion);

    KnowledgeDocument latestBySourceAndChecksum(UUID sourceId, String checksumSha256);
}
