package com.ban.vehicle_management.domain.ai.knowledge.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Upload-time file rules: allowed extensions, canonical MIME types and failure codes
 * shared by the upload pipeline and the admin UI. The backend never trusts the client
 * Content-Type; it resolves the canonical MIME from the declared extension after the
 * content signature/codec checks pass.
 */
public final class KnowledgeDocumentFilePolicy {

    private KnowledgeDocumentFilePolicy() {
    }

    public static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String EMPTY_FILE = "EMPTY_FILE";
    public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    public static final String MACRO_EMBEDDED_DOCUMENT = "MACRO_EMBEDDED_DOCUMENT";
    public static final String ENCRYPTED_DOCUMENT = "ENCRYPTED_DOCUMENT";
    public static final String ZIP_BOMB_DETECTED = "ZIP_BOMB_DETECTED";
    public static final String TENANT_CONTEXT_NOT_SUPPORTED = "TENANT_CONTEXT_NOT_SUPPORTED";

    public static final String NO_EXTRACTABLE_TEXT = "NO_EXTRACTABLE_TEXT";
    public static final String EXTRACTION_FAILED = "EXTRACTION_FAILED";
    public static final String CHUNKING_FAILED = "CHUNKING_FAILED";
    public static final String EMBEDDING_FAILED = "EMBEDDING_FAILED";

    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private static final Map<String, String> EXTENSION_MIME = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("md", "text/markdown"),
            Map.entry("txt", "text/plain")
    );

    private static final Set<String> ALLOWED_EXTENSIONS = EXTENSION_MIME.keySet();

    public static Set<String> allowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    /** Canonical MIME for the extension, ignoring case. */
    public static Optional<String> canonicalMimeType(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(EXTENSION_MIME.get(extension.trim().toLowerCase(Locale.ROOT)));
    }

    public static boolean isSupported(String extension) {
        return canonicalMimeType(extension).isPresent();
    }

    /**
     * Extracts the extension from an original file name, stripping path segments and
     * rejecting overlong or control-character pollutions.
     */
    public static String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new BadRequestException("knowledge.file.originalFilenameRequired");
        }
        String safe = originalFilename
                .replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        String name = slash >= 0 ? safe.substring(slash + 1) : safe;
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            throw new BadRequestException("knowledge.file.extensionRequired");
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (extension.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("knowledge.file.invalidExtension");
        }
        return extension;
    }
}
