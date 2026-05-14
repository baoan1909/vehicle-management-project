package com.ban.vehicle_management.infrastructure.persistence.people.customer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
}
