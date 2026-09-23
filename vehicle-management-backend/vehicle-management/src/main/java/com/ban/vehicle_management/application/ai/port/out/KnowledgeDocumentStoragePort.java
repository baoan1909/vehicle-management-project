package com.ban.vehicle_management.application.ai.port.out;

import java.io.InputStream;
import java.util.UUID;

/**
 * Object storage for knowledge documents. Uploads are stored before the ingest job is
 * scheduled; workers read the object stream directly instead of a presigned URL.
 */
public interface KnowledgeDocumentStoragePort {

    /**
     * Stores a document in the private knowledge bucket and returns the generated object key.
     */
    String store(byte[] content, String contentType, UUID documentId, String extension);

    InputStream openRead(String objectKey);

    boolean exists(String objectKey);

    long size(String objectKey);

    void delete(String objectKey);
}