package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerFavoriteParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerFavoriteParkingLotId;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerFavoriteParkingLotRepository
        extends JpaRepository<CustomerFavoriteParkingLotEntity, CustomerFavoriteParkingLotId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO people.customer_favorite_parking_lots (
                customer_id,
                parking_lot_id,
                is_favorite,
                favorited_at,
                unfavorited_at,
                created_at,
                created_by,
                updated_at,
                updated_by
            ) VALUES (
                :customerId,
                :parkingLotId,
                :favorite,
                :favoritedAt,
                :unfavoritedAt,
                :createdAt,
                :createdBy,
                :updatedAt,
                :updatedBy
            )
            ON CONFLICT (customer_id, parking_lot_id) DO UPDATE SET
                is_favorite = EXCLUDED.is_favorite,
                favorited_at = EXCLUDED.favorited_at,
                unfavorited_at = EXCLUDED.unfavorited_at,
                updated_at = EXCLUDED.updated_at,
                updated_by = EXCLUDED.updated_by
            """, nativeQuery = true)
    void upsert(
            @Param("customerId") UUID customerId,
            @Param("parkingLotId") UUID parkingLotId,
            @Param("favorite") boolean favorite,
            @Param("favoritedAt") Instant favoritedAt,
            @Param("unfavoritedAt") Instant unfavoritedAt,
            @Param("createdAt") Instant createdAt,
            @Param("createdBy") UUID createdBy,
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    @Query("""
            select favoriteParkingLot.id.parkingLotId
            from CustomerFavoriteParkingLotEntity favoriteParkingLot
            where favoriteParkingLot.id.customerId = :customerId
              and favoriteParkingLot.favorite = true
            order by favoriteParkingLot.favoritedAt desc
            """)
    Set<UUID> findFavoriteParkingLotIdsByCustomerId(@Param("customerId") UUID customerId);
}

