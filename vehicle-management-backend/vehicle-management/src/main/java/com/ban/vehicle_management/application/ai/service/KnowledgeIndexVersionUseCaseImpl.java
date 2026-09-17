package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.KnowledgeIndexVersionPortIn;
import com.ban.vehicle_management.application.ai.command.CreateKnowledgeIndexVersionCommand;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeEmbeddingPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.policy.KnowledgeIndexVersionPolicy;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeIndexVersionUseCaseImpl implements KnowledgeIndexVersionPortIn {

    public static final String CHUNKER_VERSION = "chunker-v1";
    public static final String DISTANCE_METRIC = "COSINE";
    public static final String NORMALIZATION = "NONE";
    public static final int SUPPORTED_DIMENSION = 768;

    private final CurrentAccountPortIn currentAccountPortIn;
    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final KnowledgeChunkPortOut chunkPortOut;
    private final KnowledgeEmbeddingPortOut embeddingPortOut;
    private final EmbeddingPromptFormatter promptFormatter;
    private final EmbeddingProperties embeddingProperties;

    public KnowledgeIndexVersionUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            KnowledgeChunkPortOut chunkPortOut,
            KnowledgeEmbeddingPortOut embeddingPortOut,
            EmbeddingPromptFormatter promptFormatter,
            EmbeddingProperties embeddingProperties
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.chunkPortOut = chunkPortOut;
        this.embeddingPortOut = embeddingPortOut;
        this.promptFormatter = promptFormatter;
        this.embeddingProperties = embeddingProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexVersion> listIndexVersions() {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return indexVersionPortOut.findAll();
    }

    @Override
    @Transactional
    public KnowledgeIndexVersion createDraft(CreateKnowledgeIndexVersionCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        AiModelConfiguration configuration = configurationPortOut.findById(command.modelConfigurationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình model AI"));
        if (configuration.getUseCase() != AiUseCase.EMBEDDING) {
            throw new BadRequestException("Cấu hình được chọn không phải model embedding");
        }
        Integer dimension = configuration.getOutputDimension();
        if (dimension == null || dimension != SUPPORTED_DIMENSION) {
            throw new BadRequestException("Output dimension của model embedding phải là " + SUPPORTED_DIMENSION);
        }
        String code = command.versionCode() == null || command.versionCode().isBlank()
                ? generatedCode(configuration.getModelId())
                : TextValidationUtils.normalizeCode(command.versionCode(), "versionCode", 120);
        KnowledgeIndexVersion draft = KnowledgeIndexVersion.draft(
                code,
                configuration.getConfigurationId(),
                configuration.getProvider(),
                configuration.getModelId(),
                dimension,
                CHUNKER_VERSION,
                promptFormatter.version(),
                DISTANCE_METRIC,
                NORMALIZATION,
                null,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now()
        );
        return indexVersionPortOut.save(draft);
    }

    @Override
    @Transactional
    public KnowledgeIndexVersion startBuild(UUID indexVersionId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_REINDEX_ALL");
        if (!embeddingProperties.isEnabled()) {
            throw new BadRequestException("Tính năng embedding đang tắt, không thể bắt đầu lập chỉ mục");
        }
        KnowledgeIndexVersion version = lock(indexVersionId);
        validateModelConfiguration(version);
        long eligibleChunkCount = chunkPortOut.countEligibleChunks();
        if (eligibleChunkCount <= 0) {
            throw new BadRequestException("Kho tri thức chưa có dữ liệu sẵn sàng để lập chỉ mục");
        }
        version.setExpectedChunkCount(capCount(eligibleChunkCount));
        version.setContentChecksum(chunkPortOut.calculateEligibleCorpusChecksum());
        version.startBuild(currentAccountPortIn.getCurrentAccountIdOrThrow(), Instant.now());
        return indexVersionPortOut.save(version);
    }

    @Override
    @Transactional
    public KnowledgeIndexVersion activate(UUID indexVersionId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_APPROVE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        KnowledgeIndexVersion target = lock(indexVersionId);
        KnowledgeIndexVersionPolicy.assertTransition(target.getStatus(), KnowledgeIndexVersionStatus.ACTIVE);
        validateReadyForActivation(target);
        KnowledgeIndexVersion current = indexVersionPortOut.findActiveForUpdate().orElse(null);
        if (current != null) {
            current.retire(actor, now);
            indexVersionPortOut.save(current);
        }
        target.activate(actor, now);
        return indexVersionPortOut.save(target);
    }

    @Override
    @Transactional
    public KnowledgeIndexVersion rollback(UUID indexVersionId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_APPROVE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        KnowledgeIndexVersion target = lock(indexVersionId);
        if (target.getStatus() != KnowledgeIndexVersionStatus.RETIRED || target.getActivatedAt() == null) {
            throw new BadRequestException("Chỉ có thể quay lại một phiên bản đã từng hoạt động");
        }
        validateReadyForActivation(target);
        KnowledgeIndexVersion current = indexVersionPortOut.findActiveForUpdate()
                .orElseThrow(() -> new BadRequestException("Không có index đang hoạt động để quay lại"));
        current.retire(actor, now);
        indexVersionPortOut.save(current);
        target.activate(actor, now);
        return indexVersionPortOut.save(target);
    }

    private void validateReadyForActivation(KnowledgeIndexVersion target) {
        if (target.getExpectedChunkCount() <= 0) {
            throw new BadRequestException("Index chưa có dữ liệu để kích hoạt");
        }
        if (target.getFailedChunkCount() > 0) {
            throw new BadRequestException("Index có chunk thất bại, không thể kích hoạt");
        }
        long embeddedCount = embeddingPortOut.countEmbeddedByIndexVersion(target.getIndexVersionId());
        if (embeddedCount < target.getExpectedChunkCount()) {
            throw new BadRequestException("Index chưa đạt tỷ lệ phủ sóng yêu cầu, không thể kích hoạt");
        }
        if (target.getDimension() != SUPPORTED_DIMENSION) {
            throw new BadRequestException("Dimension embedding phải là " + SUPPORTED_DIMENSION + " để kích hoạt");
        }
        if (embeddingPortOut.countInvalidVectors(target.getIndexVersionId()) > 0) {
            throw new BadRequestException("Phát hiện vector embedding không hợp lệ, không thể kích hoạt");
        }
        if (!java.util.Objects.equals(
                target.getContentChecksum(),
                chunkPortOut.calculateEligibleCorpusChecksum()
        )) {
            throw new BadRequestException("Kho tri thức đã thay đổi, cần lập phiên bản chỉ mục mới");
        }
        validateModelConfiguration(target);
    }

    private void validateModelConfiguration(KnowledgeIndexVersion target) {
        AiModelConfiguration configuration = configurationPortOut.findById(target.getModelConfigurationId())
                .orElseThrow(() -> new BadRequestException("Cấu hình model embedding không tồn tại"));
        if (configuration.getUseCase() != AiUseCase.EMBEDDING || configuration.getOutputDimension() == null) {
            throw new BadRequestException("Cấu hình model embedding không hợp lệ");
        }
        if (configuration.getStatus() == AiModelStatus.DISABLED) {
            throw new BadRequestException("Cấu hình model embedding đã bị vô hiệu hóa");
        }
        if (configuration.getOutputDimension() != target.getDimension()) {
            throw new BadRequestException("Cấu hình model đã thay đổi dimension, không thể kích hoạt index cũ");
        }
        if (configuration.getProvider() != target.getProvider()
                || !configuration.getModelId().equals(target.getModelId())) {
            throw new BadRequestException("Cấu hình provider/model đã thay đổi, cần tạo phiên bản index mới");
        }
        if (!promptFormatter.supports(target.getEmbeddingPromptVersion())) {
            throw new BadRequestException("Phiên bản định dạng embedding của index không còn được hỗ trợ");
        }
    }

    private KnowledgeIndexVersion lock(UUID indexVersionId) {
        return indexVersionPortOut.findByIdForUpdate(indexVersionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản index"));
    }

    private String generatedCode(String modelId) {
        String modelSlug = modelId == null ? "embedding" : modelId.replaceAll("[^A-Za-z0-9-]", "-").toUpperCase();
        return "IDX-" + modelSlug + "-" + Instant.now().toEpochMilli();
    }

    private int capCount(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
