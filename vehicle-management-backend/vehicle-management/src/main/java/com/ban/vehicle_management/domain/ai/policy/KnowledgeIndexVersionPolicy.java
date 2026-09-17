package com.ban.vehicle_management.domain.ai.policy;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;

/**
 * State machine for knowledge index versions. A version can never jump straight to
 * ACTIVE, cannot be re-activated from FAILED, and cannot be rebuilt from ACTIVE.
 */
public final class KnowledgeIndexVersionPolicy {

    private KnowledgeIndexVersionPolicy() {
    }

    public static boolean canTransition(KnowledgeIndexVersionStatus from, KnowledgeIndexVersionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case DRAFT -> to == KnowledgeIndexVersionStatus.BUILDING;
            case BUILDING -> to == KnowledgeIndexVersionStatus.READY || to == KnowledgeIndexVersionStatus.FAILED;
            case READY -> to == KnowledgeIndexVersionStatus.ACTIVE;
            case ACTIVE -> to == KnowledgeIndexVersionStatus.RETIRED;
            case RETIRED -> to == KnowledgeIndexVersionStatus.ACTIVE;
            case FAILED -> false;
        };
    }

    public static void assertTransition(KnowledgeIndexVersionStatus from, KnowledgeIndexVersionStatus to) {
        if (!canTransition(from, to)) {
            throw new BadRequestException("Không thể chuyển trạng thái index từ " + from + " sang " + to);
        }
    }
}