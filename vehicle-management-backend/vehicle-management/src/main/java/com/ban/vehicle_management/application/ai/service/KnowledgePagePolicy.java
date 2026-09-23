package com.ban.vehicle_management.application.ai.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class KnowledgePagePolicy {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private KnowledgePagePolicy() {
    }

    static Pageable normalize(Pageable pageable) {
        int page = pageable == null || pageable.isUnpaged() ? 0 : Math.max(0, pageable.getPageNumber());
        int requestedSize = pageable == null || pageable.isUnpaged() ? DEFAULT_SIZE : pageable.getPageSize();
        int size = Math.min(MAX_SIZE, Math.max(1, requestedSize));
        return PageRequest.of(page, size, pageable == null ? org.springframework.data.domain.Sort.unsorted() : pageable.getSort());
    }
}
