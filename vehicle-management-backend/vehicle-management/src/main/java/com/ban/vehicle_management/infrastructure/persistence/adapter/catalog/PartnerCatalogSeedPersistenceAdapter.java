package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates independent Partner catalog rows in the organization's creation transaction. */
@Component
public class PartnerCatalogSeedPersistenceAdapter implements PartnerCatalogSeedPortOut {

    private final JdbcTemplate jdbcTemplate;

    public PartnerCatalogSeedPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void copyInternalCatalog(UUID organizationId) {
        jdbcTemplate.update("""
                insert into catalog.vehicle_types
                    (vehicle_type_id, organization_id, canonical_vehicle_type_id,
                     code, name, description, is_active, created_at)
                select gen_random_uuid(), ?, source.vehicle_type_id,
                       source.code, source.name, source.description, source.is_active, now()
                from catalog.vehicle_types source
                join iam.organizations template on template.organization_id = source.organization_id
                where template.code = 'COPARKING_INTERNAL'
                """, organizationId);
        jdbcTemplate.update("""
                insert into catalog.ticket_types
                    (ticket_type_id, organization_id, code, name, description, duration_days, status, created_at)
                select gen_random_uuid(), ?, source.code, source.name, source.description,
                       source.duration_days, source.status, now()
                from catalog.ticket_types source
                join iam.organizations template on template.organization_id = source.organization_id
                where template.code = 'COPARKING_INTERNAL'
                """, organizationId);
        jdbcTemplate.update("""
                insert into catalog.price_plans
                    (price_plan_id, organization_id, code, name, description, applies_to,
                     effective_from, effective_to, is_active, created_at)
                select gen_random_uuid(), ?, source.code, source.name, source.description,
                       source.applies_to, source.effective_from, source.effective_to, source.is_active, now()
                from catalog.price_plans source
                join iam.organizations template on template.organization_id = source.organization_id
                where template.code = 'COPARKING_INTERNAL'
                """, organizationId);
        jdbcTemplate.update("""
                insert into catalog.price_rules
                    (price_rule_id, organization_id, price_plan_id, vehicle_type_id, ticket_type_id,
                     rule_name, time_from, time_to, base_price, unit, lost_card_fee, priority, is_active, created_at)
                select gen_random_uuid(), ?, target_plan.price_plan_id, target_vehicle.vehicle_type_id,
                       target_ticket.ticket_type_id, source.rule_name, source.time_from, source.time_to,
                       source.base_price, source.unit, source.lost_card_fee, source.priority, source.is_active, now()
                from catalog.price_rules source
                join iam.organizations template on template.organization_id = source.organization_id
                join catalog.price_plans source_plan on source_plan.price_plan_id = source.price_plan_id
                join catalog.vehicle_types source_vehicle on source_vehicle.vehicle_type_id = source.vehicle_type_id
                left join catalog.ticket_types source_ticket on source_ticket.ticket_type_id = source.ticket_type_id
                join catalog.price_plans target_plan on target_plan.organization_id = ?
                    and target_plan.code = source_plan.code
                join catalog.vehicle_types target_vehicle on target_vehicle.organization_id = ?
                    and target_vehicle.code = source_vehicle.code
                left join catalog.ticket_types target_ticket on target_ticket.organization_id = ?
                    and target_ticket.code = source_ticket.code
                where template.code = 'COPARKING_INTERNAL'
                """, organizationId, organizationId, organizationId, organizationId);
    }
}
