package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.cache.model.CachedCitation;
import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.application.ai.cache.model.CachedHybridRow;
import com.ban.vehicle_management.application.ai.cache.model.CachedRetrievalPayload;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Structural mappings between domain and cache models. Redis adapters never
 * expose cache DTOs to controllers.
 */
@Mapper(componentModel = "spring")
public interface AiCacheMapper {

    CachedHybridRow toCachedRow(HybridSearchRow row);

    List<CachedHybridRow> toCachedRows(List<HybridSearchRow> rows);

    HybridSearchRow toDomainRow(CachedHybridRow row);

    List<HybridSearchRow> toDomainRows(List<CachedHybridRow> rows);

    CachedCitation toCachedCitation(AiMessageCitation citation);

    default KnowledgeSearchResult toSearchResult(CachedHybridRow row) {
        if (row == null) {
            return null;
        }
        return new KnowledgeSearchResult(
                row.documentId(), row.chunkId(), row.title(), row.content(), row.summary(),
                row.sourcePage(), row.sourceSection(), row.fusedScore());
    }

    default List<KnowledgeSearchResult> toSearchResults(List<CachedHybridRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().map(this::toSearchResult).toList();
    }

    default CachedRetrievalPayload copyRetrieval(CachedRetrievalPayload payload) {
        return payload;
    }

    default CachedGroundedAnswer copyAnswer(CachedGroundedAnswer answer) {
        return answer;
    }
}
