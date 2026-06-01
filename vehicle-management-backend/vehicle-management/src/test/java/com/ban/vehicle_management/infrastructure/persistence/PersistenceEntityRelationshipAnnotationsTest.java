package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.CardTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PricePlanEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.LoginAttemptEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RefreshTokenEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.PermissionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RolePermissionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.NotificationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftAssignmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersistenceEntityRelationshipAnnotationsTest {

    @Test
    void shouldUseExpectedCoreRelationshipAnnotations() throws Exception {
        assertFieldAnnotation(AccountEntity.class, "userProfile", OneToOne.class);
        assertFieldAnnotation(AccountEntity.class, "role", ManyToOne.class);
        assertJoinColumnIsReadOnly(AccountEntity.class, "role", "role_id");

        assertFieldAnnotation(UserProfileEntity.class, "account", OneToOne.class);
        assertFieldAnnotation(CustomerEntity.class, "userProfile", OneToOne.class);
        assertFieldAnnotation(EmployeeEntity.class, "userProfile", OneToOne.class);

        assertFieldAnnotation(RoleEntity.class, "rolePermissions", OneToMany.class);
        assertFieldAnnotation(RolePermissionEntity.class, "role", ManyToOne.class);
        assertFieldAnnotation(RolePermissionEntity.class, "permission", ManyToOne.class);

        assertFieldAnnotation(PricePlanEntity.class, "priceRules", OneToMany.class);
        assertFieldAnnotation(PriceRuleEntity.class, "pricePlan", ManyToOne.class);
        assertFieldAnnotation(PriceRuleEntity.class, "ticketType", ManyToOne.class);

        assertFieldAnnotation(CardEntity.class, "cardType", ManyToOne.class);
        assertFieldAnnotation(SubscriptionEntity.class, "customer", ManyToOne.class);
        assertFieldAnnotation(SubscriptionEntity.class, "card", ManyToOne.class);

        assertFieldAnnotation(ParkingLotEntity.class, "zones", OneToMany.class);
        assertFieldAnnotation(ZoneEntity.class, "parkingLot", ManyToOne.class);
        assertFieldAnnotation(ParkingSessionEntity.class, "parkingEvents", OneToMany.class);
        assertFieldAnnotation(ParkingEventEntity.class, "parkingSession", ManyToOne.class);

        assertFieldAnnotation(InvoiceEntity.class, "payments", OneToMany.class);
        assertFieldAnnotation(PaymentEntity.class, "invoice", ManyToOne.class);
        assertFieldAnnotation(ShiftEntity.class, "shiftAssignments", OneToMany.class);
        assertFieldAnnotation(ShiftAssignmentEntity.class, "shift", ManyToOne.class);
    }

    @Test
    void shouldAvoidDirectManyToManyMappings() {
        List<Class<?>> entityClasses = List.of(
                AccountEntity.class,
                AccountStatusHistoryEntity.class,
                LoginAttemptEntity.class,
                RefreshTokenEntity.class,
                RoleEntity.class,
                PermissionEntity.class,
                RolePermissionEntity.class,
                UserProfileEntity.class,
                CustomerEntity.class,
                EmployeeEntity.class,
                CustomerVehicleEntity.class,
                CardTypeEntity.class,
                TicketTypeEntity.class,
                PricePlanEntity.class,
                PriceRuleEntity.class,
                VehicleTypeEntity.class,
                CardEntity.class,
                SubscriptionEntity.class,
                LostCardReportEntity.class,
                ParkingLotEntity.class,
                ZoneEntity.class,
                LaneEntity.class,
                DeviceEntity.class,
                ParkingSessionEntity.class,
                ParkingEventEntity.class,
                InvoiceEntity.class,
                PaymentEntity.class,
                ShiftEntity.class,
                ShiftAssignmentEntity.class,
                ApprovalRequestEntity.class,
                SupportTicketEntity.class,
                NotificationEntity.class,
                AuditLogEntity.class
        );

        for (Class<?> entityClass : entityClasses) {
            for (Field field : entityClass.getDeclaredFields()) {
                assertFalse(
                        field.isAnnotationPresent(ManyToMany.class),
                        () -> entityClass.getSimpleName() + "." + field.getName() + " should not use @ManyToMany directly"
                );
            }
        }
    }

    private static void assertFieldAnnotation(Class<?> type, String fieldName, Class<? extends Annotation> annotationType)
            throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        assertTrue(
                field.isAnnotationPresent(annotationType),
                () -> type.getSimpleName() + "." + fieldName + " should be annotated with @" + annotationType.getSimpleName()
        );
    }

    private static void assertJoinColumnIsReadOnly(Class<?> type, String fieldName, String expectedColumnName)
            throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn, () -> type.getSimpleName() + "." + fieldName + " should declare @JoinColumn");
        assertEquals(expectedColumnName, joinColumn.name());
        assertFalse(joinColumn.insertable());
        assertFalse(joinColumn.updatable());
    }
}

