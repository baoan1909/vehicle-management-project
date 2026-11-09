package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class AccountPersistenceContextRepository {
    private final EntityManager entityManager;

    public AccountPersistenceContextRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void clear() {
        entityManager.clear();
    }
}
