package com.ban.vehicle_management.infrastructure.persistence.people.userprofile;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
}
