package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Visibility scope of a knowledge item. TENANT_PRIVATE may only be retrieved
 * with an application-bound tenant context and its dedicated permission.
 */
public enum KnowledgeAccessScope {
    PUBLIC,
    CUSTOMER,
    EMPLOYEE,
    ADMIN,
    TENANT_PRIVATE
}
