package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestSupportRequestPortOut;
import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.GuestSupportRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.GuestSupportRequestRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GuestSupportRequestPersistenceAdapter implements GuestSupportRequestPortOut {
    private final GuestSupportRequestRepository repository;

    public GuestSupportRequestPersistenceAdapter(GuestSupportRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public GuestSupportRequest save(GuestSupportRequest request) {
        return toDomain(repository.saveAndFlush(toEntity(request)));
    }

    @Override
    public Optional<GuestSupportRequest> findByContactHashAndIdempotencyHash(String contactHash, String idempotencyHash) {
        return repository.findByContactHashAndIdempotencyHash(contactHash, idempotencyHash).map(this::toDomain);
    }

    @Override
    public Optional<GuestSupportRequest> findByPublicReferenceCode(String publicReferenceCode) {
        return repository.findByPublicReferenceCode(publicReferenceCode).map(this::toDomain);
    }

    @Override
    public long countByIpHashSince(String ipHash, Instant since) {
        return repository.countByIpHashAndCreatedAtGreaterThanEqual(ipHash, since);
    }

    @Override
    public long countByContactHashSince(String contactHash, Instant since) {
        return repository.countByContactHashAndCreatedAtGreaterThanEqual(contactHash, since);
    }

    @Override
    public long countBySessionHashSince(String sessionHash, Instant since) {
        return repository.countBySessionHashAndCreatedAtGreaterThanEqual(sessionHash, since);
    }

    @Override
    public void lockContact(String contactHash) {
        repository.lockContact(contactHash);
    }

    @Override
    public long deleteExpiredBefore(Instant now) {
        return repository.deleteByRetentionExpiresAtBefore(now);
    }

    private GuestSupportRequestEntity toEntity(GuestSupportRequest model) {
        GuestSupportRequestEntity entity = new GuestSupportRequestEntity();
        entity.setRequestId(model.getRequestId());
        entity.setPublicReferenceCode(model.getPublicReferenceCode());
        entity.setCategoryId(model.getCategoryId());
        entity.setTitle(model.getTitle());
        entity.setContent(model.getContent());
        entity.setEmail(model.getEmail());
        entity.setPhone(model.getPhone());
        entity.setContactHash(model.getContactHash());
        entity.setIpHash(model.getIpHash());
        entity.setSessionHash(model.getSessionHash());
        entity.setIdempotencyHash(model.getIdempotencyHash());
        entity.setCapabilityTokenHash(model.getCapabilityTokenHash());
        entity.setCapabilityExpiresAt(model.getCapabilityExpiresAt());
        entity.setRetentionExpiresAt(model.getRetentionExpiresAt());
        entity.setStatus(model.getStatus());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }

    private GuestSupportRequest toDomain(GuestSupportRequestEntity entity) {
        GuestSupportRequest model = new GuestSupportRequest();
        model.setRequestId(entity.getRequestId());
        model.setPublicReferenceCode(entity.getPublicReferenceCode());
        model.setCategoryId(entity.getCategoryId());
        model.setTitle(entity.getTitle());
        model.setContent(entity.getContent());
        model.setEmail(entity.getEmail());
        model.setPhone(entity.getPhone());
        model.setContactHash(entity.getContactHash());
        model.setIpHash(entity.getIpHash());
        model.setSessionHash(entity.getSessionHash());
        model.setIdempotencyHash(entity.getIdempotencyHash());
        model.setCapabilityTokenHash(entity.getCapabilityTokenHash());
        model.setCapabilityExpiresAt(entity.getCapabilityExpiresAt());
        model.setRetentionExpiresAt(entity.getRetentionExpiresAt());
        model.setStatus(entity.getStatus());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }
}
