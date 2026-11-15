package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartnerCatalogDraftPersistenceIT extends PostgresIntegrationTestSupport {
    @Autowired PartnerCatalogSeedPersistenceAdapter seed;
    @Autowired ParkingLotCatalogAvailabilityPersistenceAdapter availability;
    @Autowired VehicleTypeRepository vehicles;
    @Autowired PriceRuleRepository rules;
    @Autowired ParkingLotRepository lots;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void copiesEverySourceAsDraftWithExactDependencyLineageAndDoesNotReseed() {
        UUID organization = organization();
        seed.copyInternalCatalog(organization);
        for (String table : new String[]{"vehicle_types", "ticket_types", "price_plans", "price_rules"}) {
            long sourceCount = jdbc.queryForObject("select count(*) from catalog." + table
                    + " item join iam.organizations org using (organization_id) where org.code='COPARKING_INTERNAL'", Long.class);
            assertTrue(sourceCount > 0);
            assertEquals(sourceCount, count(table, organization));
            String state = table.equals("ticket_types") ? "status" : "lifecycle_status";
            assertEquals(0L, jdbc.queryForObject("select count(*) from catalog." + table
                    + " where organization_id=? and " + state + " <> 'DRAFT'", Long.class, organization));
        }
        assertEquals(0L, jdbc.queryForObject("""
                select count(*) from catalog.price_rules copy
                join catalog.price_rules source on source.price_rule_id=copy.source_catalog_id
                join catalog.price_plans plan on plan.price_plan_id=copy.price_plan_id
                join catalog.vehicle_types vehicle on vehicle.vehicle_type_id=copy.vehicle_type_id
                left join catalog.ticket_types ticket on ticket.ticket_type_id=copy.ticket_type_id
                where copy.organization_id=? and (plan.source_catalog_id <> source.price_plan_id
                    or vehicle.source_catalog_id <> source.vehicle_type_id
                    or ticket.source_catalog_id is distinct from source.ticket_type_id)
                """, Long.class, organization));
        long before = count("price_rules", organization);
        UUID ticket = id("ticket_types", "ticket_type_id", organization, "MONTHLY");
        jdbc.update("update catalog.ticket_types set name='Partner edited draft' where ticket_type_id=?", ticket);
        seed.copyInternalCatalog(organization);
        assertEquals(before, count("price_rules", organization));
        assertEquals("Partner edited draft", jdbc.queryForObject("select name from catalog.ticket_types where ticket_type_id=?", String.class, ticket));
        assertTrue(jdbc.queryForObject("select count(*) from audit.audit_logs where new_data ->> 'organization_id' = ?", Long.class, organization.toString()) > 0);
    }

    @Test
    void duplicateSourceTicketCodesDoNotMultiplyOrMislinkCopiedRules() {
        UUID internal = jdbc.queryForObject("select organization_id from iam.organizations where code='COPARKING_INTERNAL'", UUID.class);
        UUID duplicate = UUID.randomUUID();
        jdbc.update("insert into catalog.ticket_types(ticket_type_id, organization_id, code, name, duration_days, status) values (?,?,'MONTHLY','Old monthly',30,'INACTIVE')", duplicate, internal);
        UUID rule = UUID.randomUUID();
        jdbc.update("""
                insert into catalog.price_rules(price_rule_id,organization_id,price_plan_id,vehicle_type_id,ticket_type_id,
                    rule_name,base_price,unit,lost_card_fee,priority,is_active)
                select ?,organization_id,price_plan_id,vehicle_type_id,?,'Legacy monthly version',base_price,unit,lost_card_fee,priority,false
                from catalog.price_rules where organization_id=? and time_from is null limit 1
                """, rule, duplicate, internal);
        UUID organization = organization();
        seed.copyInternalCatalog(organization);
        assertEquals(1L, jdbc.queryForObject("""
                select count(*) from catalog.price_rules rule join catalog.ticket_types ticket using(ticket_type_id)
                where rule.organization_id=? and rule.source_catalog_id=? and ticket.source_catalog_id=?
                """, Long.class, organization, rule, duplicate));
    }

    @Test
    void draftCannotBePricedAndNewLotRequiresExplicitServicesAndCompleteVisitorPricing() {
        UUID organization = organization();
        seed.copyInternalCatalog(organization);
        UUID vehicle = id("vehicle_types", "vehicle_type_id", organization, "MOTORBIKE");
        UUID ticket = id("ticket_types", "ticket_type_id", organization, "DAILY");
        UUID lot = UUID.randomUUID();
        jdbc.update("insert into parking.parking_lots(parking_lot_id,organization_id,code,name,total_capacity,status) values (?,?,?, 'Draft test lot',10,'SETUP')", lot, organization, "DRAFT_" + lot.toString().substring(0, 8));
        assertTrue(availability.findExcludedVehicleTypeIds(lot).contains(vehicle));
        assertFalse(availability.isVehicleTypeEnabled(lot, vehicle));
        assertFalse(lots.hasPublishedCatalogForActivation(lot));
        assertTrue(rules.findActiveVisitorRuleByTime(vehicle, LocalDate.now(), LocalTime.NOON).isEmpty());
        jdbc.update("update catalog.vehicle_types set lifecycle_status='ACTIVE' where vehicle_type_id=?", vehicle);
        jdbc.update("update catalog.ticket_types set status='ACTIVE' where ticket_type_id=?", ticket);
        jdbc.update("update catalog.price_plans set lifecycle_status='ACTIVE', effective_from=current_date-1, effective_to=current_date+1 where organization_id=?", organization);
        availability.setVehicleTypeEnabled(organization, lot, vehicle, true);
        availability.setTicketTypeEnabled(organization, lot, ticket, true);
        UUID canonical = jdbc.queryForObject("select canonical_vehicle_type_id from catalog.vehicle_types where vehicle_type_id=?", UUID.class, vehicle);
        assertTrue(availability.isVehicleTypeEnabled(lot, canonical));
        assertFalse(lots.hasPublishedCatalogForActivation(lot));
        jdbc.update("update catalog.price_rules set lifecycle_status='ACTIVE' where organization_id=? and vehicle_type_id=? and ticket_type_id=?", organization, vehicle, ticket);
        assertTrue(lots.hasPublishedCatalogForActivation(lot));
        assertTrue(rules.findActiveVisitorRuleByTime(vehicle, LocalDate.now(), LocalTime.NOON).isPresent());
        jdbc.update("update catalog.price_rules set lifecycle_status='INACTIVE' where organization_id=? and vehicle_type_id=? and time_from='20:00:00'", organization, vehicle);
        assertFalse(lots.hasPublishedCatalogForActivation(lot));
    }

    @Test
    void defaultDatabaseInsertIsDraftAndLegacyDeactivationRemainsConsistent() {
        UUID organization = organization();
        UUID vehicle = UUID.randomUUID();
        jdbc.update("insert into catalog.vehicle_types(vehicle_type_id,organization_id,code,name) values (?,?,'CUSTOM','Custom')", vehicle, organization);
        assertEquals("DRAFT", jdbc.queryForObject("select lifecycle_status from catalog.vehicle_types where vehicle_type_id=?", String.class, vehicle));
        jdbc.update("update catalog.vehicle_types set lifecycle_status='ACTIVE' where vehicle_type_id=?", vehicle);
        assertEquals(Boolean.TRUE, jdbc.queryForObject("select is_active from catalog.vehicle_types where vehicle_type_id=?", Boolean.class, vehicle));
        jdbc.update("update catalog.vehicle_types set is_active=false where vehicle_type_id=?", vehicle);
        assertEquals("INACTIVE", jdbc.queryForObject("select lifecycle_status from catalog.vehicle_types where vehicle_type_id=?", String.class, vehicle));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSeedRequestsCreateOneCompleteCatalog() throws Exception {
        UUID organization = organization();
        try (var workers = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            java.util.concurrent.Callable<Void> copy = () -> {
                start.await();
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed.copyInternalCatalog(organization));
                return null;
            };
            var first = workers.submit(copy);
            var second = workers.submit(copy);
            start.countDown();
            first.get(30, java.util.concurrent.TimeUnit.SECONDS);
            second.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(0L, jdbc.queryForObject("select count(*) from (select source_catalog_id from catalog.price_rules where organization_id=? group by source_catalog_id having count(*)>1) duplicates", Long.class, organization));
            assertTrue(count("price_rules", organization) > 0);
        } finally {
            for (String table : new String[]{"price_rules", "price_plans", "ticket_types", "vehicle_types"}) {
                jdbc.update("delete from catalog." + table + " where organization_id=?", organization);
            }
            jdbc.update("delete from audit.audit_logs where new_data ->> 'organization_id' = ?", organization.toString());
            jdbc.update("delete from iam.organizations where organization_id=?", organization);
        }
    }

    @Test
    @Transactional(readOnly = true)
    void diagnosticManifestQueriesExecuteWithoutWriting() throws Exception {
        UUID internal = jdbc.queryForObject("select organization_id from iam.organizations where code=\'COPARKING_INTERNAL\'", UUID.class);
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of("scripts/operations/partner-catalog-draft-dry-run.sql"))
                .replaceAll("(?m)^\\\\.*$", "")
                .replace("BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;", "")
                .replace("ROLLBACK;", "")
                .replace(":\'organization_id\'", "\'" + internal + "\'");
        for (String query : sql.split(";")) {
            if (!query.isBlank()) jdbc.queryForList(query);
        }
        for (String table : new String[]{"vehicle_types", "ticket_types", "price_plans", "price_rules"}) {
            String state = table.equals("ticket_types") ? "status" : "lifecycle_status";
            assertTrue(jdbc.queryForObject("select count(*) from catalog." + table + " where organization_id=? and " + state + "=\'ACTIVE\'", Long.class, internal) > 0);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackAfterCopyLeavesNoPartialCatalogOrClaim() {
        UUID organization = organization();
        try {
            assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                seed.copyInternalCatalog(organization);
                throw new IllegalStateException("Simulated failure after copying");
            }));
            for (String table : new String[]{"price_rules", "price_plans", "ticket_types", "vehicle_types"}) {
                assertEquals(0L, count(table, organization));
            }
            assertNull(jdbc.queryForObject("select catalog_seeded_at from iam.organizations where organization_id=?", java.time.OffsetDateTime.class, organization));
        } finally {
            jdbc.update("delete from iam.organizations where organization_id=?", organization);
        }
    }

    private UUID organization() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?, 'Draft integration test','ACTIVE')", id, "DRAFT_" + id);
        return id;
    }

    private long count(String table, UUID organization) {
        return jdbc.queryForObject("select count(*) from catalog." + table + " where organization_id=?", Long.class, organization);
    }

    private UUID id(String table, String idColumn, UUID organization, String code) {
        return jdbc.queryForObject("select " + idColumn + " from catalog." + table + " where organization_id=? and code=?", UUID.class, organization, code);
    }
}
