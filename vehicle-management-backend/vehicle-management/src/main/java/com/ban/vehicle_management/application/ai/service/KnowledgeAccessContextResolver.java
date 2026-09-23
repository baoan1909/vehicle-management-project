package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resolves the visibility scopes a caller may search. Scoping is driven by granted
 * permissions, never by role names: whether a caller may read the administration
 * knowledge plane is decided by the AI_KNOWLEDGE_READ_ALL permission, not by a role
 * containing "ADMIN". Tenant-private visibility additionally requires a trusted
 * tenant identifier supplied by the application, never one supplied by the model.
 */
@Component
public class KnowledgeAccessContextResolver {

    private static final List<String> PUBLIC_SCOPES = List.of(KnowledgeAccessScope.PUBLIC.name());

    private static final String CUSTOMER_PERMISSION = "AI_KNOWLEDGE_READ_CUSTOMER";
    private static final String EMPLOYEE_PERMISSION = "AI_KNOWLEDGE_READ_EMPLOYEE";
    private static final String TENANT_PERMISSION = "AI_KNOWLEDGE_READ_TENANT_PRIVATE";

    private static final Set<String> ADMIN_SCOPE_PERMISSIONS = Set.of(
            "AI_KNOWLEDGE_READ_ALL",
            "AI_KNOWLEDGE_MANAGE_ALL",
            "AI_KNOWLEDGE_APPROVE_ALL",
            "AI_KNOWLEDGE_REINDEX_ALL");

    private final CurrentAccountPortIn currentAccountPortIn;

    public KnowledgeAccessContextResolver(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public List<String> resolveScopes() {
        return resolveScopes(null);
    }

    public List<String> resolveScopes(java.util.UUID tenantId) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccount().orElse(null);
        if (account == null) {
            return PUBLIC_SCOPES;
        }
        return scopesForPermissions(account.getEffectivePermissionCodes(), tenantId);
    }

    /** Resolves scopes from an explicit permission set (system-to-system calls). */
    public List<String> scopesForPermissions(Set<String> permissionCodes) {
        return scopesForPermissions(permissionCodes, null);
    }

    /** Resolves scopes and only includes TENANT_PRIVATE with an application-bound tenant. */
    public List<String> scopesForPermissions(Set<String> permissionCodes, java.util.UUID tenantId) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return PUBLIC_SCOPES;
        }
        List<String> scopes = new ArrayList<>(PUBLIC_SCOPES);
        if (permissionCodes.stream().anyMatch(ADMIN_SCOPE_PERMISSIONS::contains)) {
            scopes.add(KnowledgeAccessScope.CUSTOMER.name());
            scopes.add(KnowledgeAccessScope.EMPLOYEE.name());
            scopes.add(KnowledgeAccessScope.ADMIN.name());
        } else {
            if (permissionCodes.contains(CUSTOMER_PERMISSION)) {
                scopes.add(KnowledgeAccessScope.CUSTOMER.name());
            }
            if (permissionCodes.contains(EMPLOYEE_PERMISSION)) {
                scopes.add(KnowledgeAccessScope.EMPLOYEE.name());
            }
        }
        if (tenantId != null && permissionCodes.contains(TENANT_PERMISSION)) {
            scopes.add(KnowledgeAccessScope.TENANT_PRIVATE.name());
        }
        return List.copyOf(scopes);
    }
}
