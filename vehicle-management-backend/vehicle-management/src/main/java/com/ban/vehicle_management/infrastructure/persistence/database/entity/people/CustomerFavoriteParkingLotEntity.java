package com.ban.vehicle_management.infrastructure.persistence.database.entity.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_favorite_parking_lots", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFavoriteParkingLotEntity extends AuditableEntity {

    @EmbeddedId
    private CustomerFavoriteParkingLotId id;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Column(name = "favorited_at", nullable = false)
    private Instant favoritedAt;

    @Column(name = "unfavorited_at")
    private Instant unfavoritedAt;
}

