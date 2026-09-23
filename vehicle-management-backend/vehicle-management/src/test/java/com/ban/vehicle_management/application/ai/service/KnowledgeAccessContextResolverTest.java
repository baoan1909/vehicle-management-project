package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeAccessContextResolverTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Test
    void knowledgeReadPermissionShouldResolvePublicAndAdminScopes() {
        when(currentAccountPortIn.getCurrentAccount())
                .thenReturn(Optional.of(account("SYSTEM_ADMIN",
                        Set.of("AI_KNOWLEDGE_READ_ALL"))));

        List<String> scopes = new KnowledgeAccessContextResolver(currentAccountPortIn).resolveScopes();

        assertEquals(List.of("PUBLIC", "CUSTOMER", "EMPLOYEE", "ADMIN"), scopes);
    }

    @Test
    void nonKnowledgePermissionShouldOnlySeePublicScope() {
        when(currentAccountPortIn.getCurrentAccount())
                .thenReturn(Optional.of(account("EMPLOYEE", Set.of("SUPPORT_WIDGET_ACCESS_OWN"))));

        List<String> scopes = new KnowledgeAccessContextResolver(currentAccountPortIn).resolveScopes();

        assertEquals(List.of("PUBLIC"), scopes);
    }

    @Test
    void roleNameWithoutKnowledgePermissionShouldNotGrantAdminScope() {
        when(currentAccountPortIn.getCurrentAccount())
                .thenReturn(Optional.of(account("ADMIN", Set.of())));

        List<String> scopes = new KnowledgeAccessContextResolver(currentAccountPortIn).resolveScopes();

        assertEquals(List.of("PUBLIC"), scopes);
    }

    @Test
    void missingAccountShouldOnlySeePublicScope() {
        when(currentAccountPortIn.getCurrentAccount()).thenReturn(Optional.empty());

        List<String> scopes = new KnowledgeAccessContextResolver(currentAccountPortIn).resolveScopes();

        assertEquals(List.of("PUBLIC"), scopes);
    }

    @Test
    void scopesNeverContainTenantPrivate() {
        when(currentAccountPortIn.getCurrentAccount())
                .thenReturn(Optional.of(account("SYSTEM_ADMIN", Set.of("AI_KNOWLEDGE_READ_ALL"))));

        List<String> scopes = new KnowledgeAccessContextResolver(currentAccountPortIn).resolveScopes();

        assertFalse(scopes.stream().anyMatch("TENANT_PRIVATE"::equals));
    }

    @Test
    void staticScopesFromPermissionsMirrorResolution() {
        KnowledgeAccessContextResolver resolver = new KnowledgeAccessContextResolver(currentAccountPortIn);
        assertEquals(List.of("PUBLIC", "CUSTOMER", "EMPLOYEE", "ADMIN"),
                resolver.scopesForPermissions(Set.of("AI_KNOWLEDGE_REINDEX_ALL")));
        assertEquals(List.of("PUBLIC", "CUSTOMER"),
                resolver.scopesForPermissions(Set.of("AI_KNOWLEDGE_READ_CUSTOMER")));
        assertEquals(List.of("PUBLIC", "EMPLOYEE"),
                resolver.scopesForPermissions(Set.of("AI_KNOWLEDGE_READ_EMPLOYEE")));
        assertEquals(List.of("PUBLIC"),
                resolver.scopesForPermissions(Set.of("HR_EMPLOYEE_VIEW")));
        assertEquals(List.of("PUBLIC"),
                resolver.scopesForPermissions(null));
        assertEquals(List.of("PUBLIC"),
                resolver.scopesForPermissions(Set.of()));
    }

    @Test
    void tenantPrivateRequiresBothPermissionAndTrustedTenantContext() {
        KnowledgeAccessContextResolver resolver = new KnowledgeAccessContextResolver(currentAccountPortIn);
        Set<String> permissions = Set.of("AI_KNOWLEDGE_READ_TENANT_PRIVATE");

        assertEquals(List.of("PUBLIC"), resolver.scopesForPermissions(permissions));
        assertEquals(List.of("PUBLIC", "TENANT_PRIVATE"),
                resolver.scopesForPermissions(permissions, UUID.randomUUID()));
    }

    private CurrentAccountAccess account(String roleCode, Set<String> permissions) {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "user",
                "user@test.local",
                UUID.randomUUID(),
                roleCode,
                AccountStatus.ACTIVE,
                null,
                permissions);
    }
}
