package com.ban.vehicle_management.infrastructure.persistence.database.specification.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class LaneSpecifications {
    private LaneSpecifications(){

    }
    public static Specification<LaneEntity> withFilters(
            UUID gateId,
            LaneDirection direction,
            LaneStatus status,
            String keyword
    ){
        return  Specification
                .where(hasGateId(gateId))
                .and(hasDirection(direction))
                .and(hasStatus(status))
                .and(containsKeyword(keyword));
    }

    private static Specification<LaneEntity> hasGateId(UUID gateId){
        return (root, query, cb) -> gateId == null ? null : cb.equal(root.get("gateId"), gateId);
    }

    private static Specification<LaneEntity> hasDirection(LaneDirection direction){
        return (root, query, cb) -> direction == null ? null : cb.equal(root.get("direction"), direction);
    }

    private static Specification<LaneEntity> hasStatus(LaneStatus status){
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<LaneEntity> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }
}
