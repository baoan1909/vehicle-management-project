package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.command.UpdateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeSourcePortIn;
import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
import com.ban.vehicle_management.application.ai.query.KnowledgeSourceQuery;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeSourcePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class KnowledgeSourceUseCaseImpl implements KnowledgeSourcePortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final KnowledgeSourcePortOut sourcePortOut;
    private final KnowledgeDocumentPortOut documentPortOut;

    public KnowledgeSourceUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            KnowledgeSourcePortOut sourcePortOut,
            KnowledgeDocumentPortOut documentPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.sourcePortOut = sourcePortOut;
        this.documentPortOut = documentPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeSource> listSources() {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return sourcePortOut.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeSource> listSources(KnowledgeSourceQuery query, Pageable pageable) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return sourcePortOut.findAll(query, KnowledgePagePolicy.normalize(pageable));
    }

    @Override
    @Transactional
    public KnowledgeSource createSource(CreateKnowledgeSourceCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        String title = TextValidationUtils.normalizeRequiredText(command.title(), "title", 200);
        String description = TextValidationUtils.normalizeNullableText(command.description(), "description", 255);
        KnowledgeAccessScope scope = command.accessScope();
        if (scope == null) {
            scope = KnowledgeAccessScope.PUBLIC;
        }
        assertSupportedScope(scope);
        if (sourcePortOut.existsByTitle(title)) {
            throw new ConflictException("Nguồn kiến thức đã tồn tại với tên '" + title + "'");
        }
        return sourcePortOut.save(KnowledgeSource.create(
                null,
                title,
                description,
                scope,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now()));
    }

    @Override
    @Transactional
    public KnowledgeSource updateSource(UUID sourceId, UpdateKnowledgeSourceCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        KnowledgeSource source = sourcePortOut.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        String title = TextValidationUtils.normalizeRequiredText(command.title(), "title", 200);
        String description = TextValidationUtils.normalizeNullableText(command.description(), "description", 255);
        if (!title.equalsIgnoreCase(source.getTitle()) && sourcePortOut.existsByTitle(title)) {
            throw new ConflictException("Nguồn kiến thức đã tồn tại với tên '" + title + "'");
        }
        KnowledgeAccessScope scope = command.accessScope() == null
                ? source.getAccessScope()
                : command.accessScope();
        assertSupportedScope(scope);
        source.rename(title, description, currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now());
        if (command.accessScope() != null && !command.accessScope().equals(source.getAccessScope())) {
            source.changeScope(scope, currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now());
        }
        return sourcePortOut.save(source);
    }

    @Override
    @Transactional
    public void deactivateSource(UUID sourceId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        KnowledgeSource source = sourcePortOut.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        source.deactivate(currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now());
        sourcePortOut.save(source);
    }

    @Override
    @Transactional
    public void reactivateSource(UUID sourceId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        KnowledgeSource source = sourcePortOut.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        source.reactivate(currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now());
        sourcePortOut.save(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> listDocuments(UUID sourceId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        sourcePortOut.findById(sourceId).orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        return documentPortOut.findBySourceId(sourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> listDocuments(UUID sourceId, Pageable pageable) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        sourcePortOut.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        KnowledgeDocumentQuery query = new KnowledgeDocumentQuery(
                sourceId, null, null, null, null, null, null, null, null);
        return documentPortOut.findAll(query, KnowledgePagePolicy.normalize(pageable));
    }

    private void assertSupportedScope(KnowledgeAccessScope scope) {
        if (scope == KnowledgeAccessScope.TENANT_PRIVATE) {
            throw new BadRequestException(
                    "TENANT_CONTEXT_NOT_SUPPORTED: Phạm vi riêng tư theo khách hàng chưa được hỗ trợ");
        }
    }
}
