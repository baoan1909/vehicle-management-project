package com.ban.vehicle_management.infrastructure.mapper.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRunEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = AiRunPersistenceMapperImpl.class)
class AiRunPersistenceMapperTest {

    @Autowired
    private AiRunPersistenceMapper mapper;

    @Test
    void newRunUsesEmptyFieldViolationsJsonArray() {
        AiRun run = new AiRun();

        assertEquals("[]", run.getFieldViolationsRedacted());
    }

    @Test
    void mapsNullFieldViolationsToEmptyJsonArray() {
        AiRun run = new AiRun();
        run.setFieldViolationsRedacted(null);

        AiRunEntity entity = mapper.toEntity(run);

        assertEquals("[]", entity.getFieldViolationsRedacted());
    }
}
