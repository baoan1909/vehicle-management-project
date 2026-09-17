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
 * containing "ADMIN". TENANT_PRIVATE is deliberately never emitted: multi-tenancy is a
 * later phase, so nothing can retrieve or index tenant-private knowledge today.
 *
 * Self-service scopes (EMPLOYEE, CUSTOMER) would require dedicated knowledge
 * permissions; none are provisioned yet, so those scopes are never emitted.
 */
@Component
public class KnowledgeAccessContextResolver {

    private static final List<String> PUBLIC_SCOPES = List.of(KnowledgeAccessScope.PUBLIC.name());

    private static final String ADMIN_SCOPE = KnowledgeAccessScope.ADMIN.name();

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
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccount().orElse(null);
        if (account == null) {
            return PUBLIC_SCOPES;
        }
        return scopesForPermissions(account.getEffectivePermissionCodes());
    }

    /** Resolves scopes from an explicit permission set (system-to-system calls). */
    public List<String> scopesForPermissions(Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return PUBLIC_SCOPES;
        }
        if (permissionCodes.stream().noneMatch(ADMIN_SCOPE_PERMISSIONS::contains)) {
            return PUBLIC_SCOPES;
        }
        List<String> scopes = new ArrayList<>(PUBLIC_SCOPES);
        scopes.add(ADMIN_SCOPE);
        return List.copyOf(scopes);
    }
}