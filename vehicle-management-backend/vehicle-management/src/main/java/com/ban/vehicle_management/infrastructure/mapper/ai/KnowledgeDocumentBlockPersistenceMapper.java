package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentBlockEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface KnowledgeDocumentBlockPersistenceMapper {

    @Mapping(target = "kind", source = "kind", qualifiedByName = "kindName")
    KnowledgeDocumentBlockEntity toEntity(KnowledgeDocumentBlock domain);

    @Mapping(target = "kind", source = "kind", qualifiedByName = "kindEnum")
    KnowledgeDocumentBlock toDomain(KnowledgeDocumentBlockEntity entity);

    List<KnowledgeDocumentBlockEntity> toEntityList(List<KnowledgeDocumentBlock> domains);

    List<KnowledgeDocumentBlock> toDomainList(List<KnowledgeDocumentBlockEntity> entities);

    @Named("kindName")
    default String kindName(KnowledgeDocumentBlock.BlockKind kind) {
        return kind == null ? null : kind.name();
    }

    @Named("kindEnum")
    default KnowledgeDocumentBlock.BlockKind kindEnum(String name) {
        return name == null ? null : KnowledgeDocumentBlock.BlockKind.valueOf(name);
    }
}