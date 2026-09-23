package com.ban.vehicle_management.application.ai.port.out;

/**
 * Byte-level security checks applied before a document is stored. Implementations run
 * signature, macro/encryption and archive-bomb inspections without trusting the MIME
 * type reported by the client.
 */
public interface KnowledgeDocumentValidationPortOut {

    void validate(byte[] content, String extension, long maxFileSizeBytes);
}