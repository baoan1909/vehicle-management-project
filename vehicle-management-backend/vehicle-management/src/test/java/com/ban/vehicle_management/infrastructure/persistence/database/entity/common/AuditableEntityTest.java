package com.ban.vehicle_management.infrastructure.persistence.database.entity.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityListeners;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

class AuditableEntityTest {

    @Test
    void shouldRegisterAuditingEntityListener() {
        EntityListeners entityListeners = AuditableEntity.class.getAnnotation(EntityListeners.class);

        assertNotNull(entityListeners);
        assertEquals(AuditingEntityListener.class, entityListeners.value()[0]);
    }

    @Test
    void shouldMarkCreatedAtAndCreatedByFieldsForAuditing() throws NoSuchFieldException {
        Field createdAtField = AuditableEntity.class.getDeclaredField("createdAt");
        Field createdByField = AuditableEntity.class.getDeclaredField("createdBy");

        assertNotNull(createdAtField.getAnnotation(CreatedDate.class));
        assertNotNull(createdByField.getAnnotation(CreatedBy.class));
    }

    @Test
    void shouldMarkUpdatedAtAndUpdatedByFieldsForAuditing() throws NoSuchFieldException {
        Field updatedAtField = AuditableEntity.class.getDeclaredField("updatedAt");
        Field updatedByField = AuditableEntity.class.getDeclaredField("updatedBy");

        assertNotNull(updatedAtField.getAnnotation(LastModifiedDate.class));
        assertNotNull(updatedByField.getAnnotation(LastModifiedBy.class));
    }
}
