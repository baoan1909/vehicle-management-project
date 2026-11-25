package com.ban.vehicle_management.ai.golden;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.service.*;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovedModelRegistryFixtureTest {
    @TempDir Path directory;
    @Test void keepsMoreThanThreeApprovedModelsAndUsesRealRouterPriorityRolloutAndFreePolicy() throws Exception {
        var rows = new ArrayList<Map<String,Object>>();
        rows.add(Map.of("modelId","primary-model","status","ACTIVE","freeTierApproved",true,"priority",10,"rolloutPercentage",100));
        for (int n=1;n<=5;n++) rows.add(Map.of("modelId","fallback-"+n,"status","FALLBACK","freeTierApproved",true,"priority",10+n,"rolloutPercentage",0));
        rows.add(Map.of("modelId","not-approved","status","FALLBACK","freeTierApproved",false));
        rows.add(Map.of("modelId","disabled","status","DISABLED","freeTierApproved",true));
        Path snapshot=directory.resolve("registry.json");new ObjectMapper().writeValue(snapshot.toFile(),Map.of("models",rows));
        var models=ApprovedModelRegistryFixture.load(snapshot,new ObjectMapper());
        var port=mock(AiModelConfigurationPortOut.class);when(port.findEnabledByUseCase(AiUseCase.SUPPORT_CHAT)).thenReturn(models);
        var router=new AiModelRouter(port,new AiModelPolicyService(new AiAssistantProperties()));
        var selected=router.resolveCandidates(AiUseCase.SUPPORT_CHAT,UUID.randomUUID(),UUID.randomUUID(),1);
        assertEquals(6,selected.size());assertEquals("primary-model",selected.getFirst().getModelId());assertEquals("fallback-5",selected.getLast().getModelId());
        assertTrue(selected.stream().allMatch(model->model.isFreeTierApproved()));
    }
    @Test void approvalIsStrictBooleanAndEmptyOrDuplicateMetadataFailsClosed() throws Exception {
        Path snapshot=directory.resolve("registry.json");var mapper=new ObjectMapper();
        mapper.writeValue(snapshot.toFile(),Map.of("models",List.of(Map.of("modelId","not-approved","status","ACTIVE","freeTierApproved","true"))));
        assertThrows(IllegalArgumentException.class,()->ApprovedModelRegistryFixture.load(snapshot,mapper));
        var row=Map.of("modelId","duplicate","status","ACTIVE","freeTierApproved",true);
        mapper.writeValue(snapshot.toFile(),Map.of("models",List.of(row,row)));
        assertThrows(IllegalArgumentException.class,()->ApprovedModelRegistryFixture.load(snapshot,mapper));
    }
}
