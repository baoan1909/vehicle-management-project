package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Visibility scope of a knowledge item. TENANT_PRIVATE is reserved for a later
 * multi-tenant phase and is currently rejected by the backend when creating a
 * source or uploading a document; retrieval never includes it.
 */
public enum KnowledgeAccessScope {
    PUBLIC,
    CUSTOMER,
    EMPLOYEE,
    ADMIN,
    TENANT_PRIVATE
}