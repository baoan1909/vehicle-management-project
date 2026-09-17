package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeIndexVersionCommand;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import java.util.List;
import java.util.UUID;

public interface KnowledgeIndexVersionPortIn {

    List<KnowledgeIndexVersion> listIndexVersions();

    KnowledgeIndexVersion createDraft(CreateKnowledgeIndexVersionCommand command);

    KnowledgeIndexVersion startBuild(UUID indexVersionId);

    KnowledgeIndexVersion activate(UUID indexVersionId);

    KnowledgeIndexVersion rollback(UUID indexVersionId);
}
