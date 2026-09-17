package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.command.UpdateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeSourcePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeSourceUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private KnowledgeSourcePortOut sourcePortOut;
    @Mock
    private KnowledgeDocumentPortOut documentPortOut;

    private KnowledgeSourceUseCaseImpl useCase;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new KnowledgeSourceUseCaseImpl(currentAccountPortIn, sourcePortOut, documentPortOut);
        lenient().when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actor);
        lenient().when(sourcePortOut.save(any(KnowledgeSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createSourceShouldDefaultToPublicScope() {
        KnowledgeSource created = useCase.createSource(
                new CreateKnowledgeSourceCommand("Hướng dẫn sử dụng", "Mô tả", null));

        assertEquals(KnowledgeAccessScope.PUBLIC, created.getAccessScope());
        assertEquals(KnowledgeSourceStatus.ACTIVE, created.getStatus());
        verify(sourcePortOut).save(any(KnowledgeSource.class));
    }

    @Test
    void createSourceShouldRejectTenantPrivateScope() {
        assertThrows(
                BadRequestException.class,
                () -> useCase.createSource(
                        new CreateKnowledgeSourceCommand("Tên", null, KnowledgeAccessScope.TENANT_PRIVATE))
        );
        verify(sourcePortOut, never()).save(any(KnowledgeSource.class));
    }

    @Test
    void createSourceShouldRejectDuplicateTitle() {
        when(sourcePortOut.existsByTitle("Trùng tên")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> useCase.createSource(
                        new CreateKnowledgeSourceCommand("Trùng tên", null, KnowledgeAccessScope.PUBLIC))
        );
    }

    @Test
    void updateSourceShouldRejectMissingSource() {
        when(sourcePortOut.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.updateSource(
                        UUID.randomUUID(),
                        new UpdateKnowledgeSourceCommand("Tên mới", null, KnowledgeAccessScope.PUBLIC))
        );
    }

    @Test
    void updateSourceShouldRenameAndKeepScopeWhenNotProvided() {
        KnowledgeSource source = KnowledgeSource.create(
                null, "Cũ", null, KnowledgeAccessScope.PUBLIC, actor, java.time.Instant.now());
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));
        when(sourcePortOut.existsByTitle("Mới")).thenReturn(false);

        KnowledgeSource updated = useCase.updateSource(
                source.getSourceId(),
                new UpdateKnowledgeSourceCommand("Mới", "mô tả", null));

        assertEquals("Mới", updated.getTitle());
        assertEquals(KnowledgeAccessScope.PUBLIC, updated.getAccessScope());
    }

    @Test
    void deactivateSourceShouldMarkInactive() {
        KnowledgeSource source = KnowledgeSource.create(
                null, "Tên", null, KnowledgeAccessScope.PUBLIC, actor, java.time.Instant.now());
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));

        useCase.deactivateSource(source.getSourceId());

        assertEquals(KnowledgeSourceStatus.INACTIVE, source.getStatus());
    }

    @Test
    void reactivateSourceShouldMarkActive() {
        KnowledgeSource source = KnowledgeSource.create(
                null, "Tên", null, KnowledgeAccessScope.PUBLIC, actor, java.time.Instant.now());
        source.deactivate(actor, java.time.Instant.now());
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));

        useCase.reactivateSource(source.getSourceId());

        assertEquals(KnowledgeSourceStatus.ACTIVE, source.getStatus());
    }

    @Test
    void listDocumentsShouldResolveSourceFirst() {
        UUID sourceId = UUID.randomUUID();
        when(sourcePortOut.findById(eq(sourceId))).thenReturn(
                Optional.of(KnowledgeSource.create(
                        null, "Tên", null, KnowledgeAccessScope.PUBLIC, actor, java.time.Instant.now())));

        useCase.listDocuments(sourceId);

        verify(documentPortOut).findBySourceId(sourceId);
    }
}