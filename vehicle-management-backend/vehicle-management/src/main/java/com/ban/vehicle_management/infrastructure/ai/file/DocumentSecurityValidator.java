package com.ban.vehicle_management.infrastructure.ai.file;

import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.shared.exception.KnowledgeFileValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

/**
 * Security gate applied before an uploaded document is stored. It validates the real
 * content against the declared extension, rejects encrypted/macro documents and
 * zip-bomb archives, and refuses binary payloads masquerading as text.
 */
@Component
public class DocumentSecurityValidator {

    private static final long MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES = 512L * 1024 * 1024;
    private static final long MAX_ZIP_ENTRY_UNCOMPRESSED_BYTES = 64L * 1024 * 1024;
    private static final int MAX_ZIP_COMPRESSION_RATIO = 200;
    private static final int MAX_ZIP_ENTRIES = 10_000;
    private static final int MAX_RELATIONSHIP_BYTES = 1024 * 1024;

    private final DocumentTypeDetector typeDetector;

    public DocumentSecurityValidator(DocumentTypeDetector typeDetector) {
        this.typeDetector = typeDetector;
    }

    public void validate(byte[] bytes, String extension, long maxFileSizeBytes) {
        if (bytes == null || bytes.length == 0) {
            throw reject(KnowledgeDocumentFilePolicy.EMPTY_FILE, "Tệp rỗng hoặc không đọc được");
        }
        if (bytes.length > maxFileSizeBytes) {
            throw reject(KnowledgeDocumentFilePolicy.FILE_TOO_LARGE,
                    "Tệp vượt quá dung lượng cho phép " + maxFileSizeBytes + " byte");
        }
        switch (extension) {
            case "pdf" -> validatePdf(bytes);
            case "docx" -> validateDocx(bytes);
            case "html", "htm" -> validateHtml(bytes);
            case "md", "txt" -> validatePlainText(bytes);
            default -> throw reject(
                    KnowledgeDocumentFilePolicy.UNSUPPORTED_FILE_TYPE,
                    "Định dạng tệp không được hỗ trợ: " + extension);
        }
    }

    private void validatePdf(byte[] bytes) {
        if (!typeDetector.isPdf(bytes)) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Nội dung tệp không phải PDF hợp lệ");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw reject(KnowledgeDocumentFilePolicy.ENCRYPTED_DOCUMENT,
                        "Tệp PDF được mã hóa, vui lòng gỡ mật khẩu trước khi tải lên");
            }
        } catch (InvalidPasswordException exception) {
            throw reject(KnowledgeDocumentFilePolicy.ENCRYPTED_DOCUMENT,
                    "Tệp PDF được mã hóa, vui lòng gỡ mật khẩu trước khi tải lên");
        } catch (KnowledgeFileValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Không đọc được cấu trúc tệp PDF");
        }
    }

    private void validateDocx(byte[] bytes) {
        if (!typeDetector.isZip(bytes)) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Nội dung tệp không phải DOCX hợp lệ");
        }
        try (ZipFile zipFile = typeDetector.openZip(bytes)) {
            if (isEncrypted(zipFile)) {
                throw reject(KnowledgeDocumentFilePolicy.ENCRYPTED_DOCUMENT,
                        "Tệp DOCX được mã hóa, vui lòng gỡ mật khẩu trước khi tải lên");
            }
            if (zipFile.getEntry("[Content_Types].xml") == null
                    || zipFile.getEntry("word/document.xml") == null) {
                throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                        "Tệp không có cấu trúc DOCX hợp lệ");
            }
            if (zipFile.getEntry("word/vbaProject.bin") != null) {
                throw reject(KnowledgeDocumentFilePolicy.MACRO_EMBEDDED_DOCUMENT,
                        "Tệp DOCX chứa macro, không được phép tải lên");
            }
            assertNotZipBomb(zipFile);
            assertNoEmbeddedOrExternalContent(zipFile);
        } catch (KnowledgeFileValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Không đọc được cấu trúc tệp DOCX");
        }
    }

    private void validateHtml(byte[] bytes) {
        if (!typeDetector.isUtf8Text(bytes)) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Nội dung HTML không phải văn bản UTF-8 hợp lệ");
        }
    }

    private void validatePlainText(byte[] bytes) {
        if (!typeDetector.isUtf8Text(bytes)) {
            throw reject(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE,
                    "Nội dung văn bản không phải UTF-8 hợp lệ");
        }
    }

    private boolean isEncrypted(ZipFile zipFile) {
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getGeneralPurposeBit().usesEncryption()) {
                return true;
            }
        }
        return false;
    }

    private void assertNotZipBomb(ZipFile zipFile) {
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        long totalUncompressed = 0;
        int entryCount = 0;
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            entryCount++;
            if (entryCount > MAX_ZIP_ENTRIES) {
                throw reject(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED,
                        "Tệp nén có quá nhiều thành phần");
            }
            long uncompressed = entry.getSize();
            long compressed = entry.getCompressedSize();
            if (uncompressed <= 0) {
                continue;
            }
            if (uncompressed > MAX_ZIP_ENTRY_UNCOMPRESSED_BYTES) {
                throw reject(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED,
                        "Tệp nén có kích thước giải nén bất thường");
            }
            if (compressed > 0 && uncompressed > 10L * 1024 * 1024
                    && (uncompressed / compressed) > MAX_ZIP_COMPRESSION_RATIO) {
                throw reject(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED,
                        "Tỷ lệ nén của tệp bất thường");
            }
            totalUncompressed += uncompressed;
            if (totalUncompressed > MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES) {
                throw reject(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED,
                        "Tổng dung lượng giải nén vượt giới hạn cho phép");
            }
        }
    }

    private void assertNoEmbeddedOrExternalContent(ZipFile zipFile) throws Exception {
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (name.startsWith("word/embeddings/") || name.contains("oleobject")) {
                throw reject(KnowledgeDocumentFilePolicy.MACRO_EMBEDDED_DOCUMENT,
                        "Tệp DOCX chứa đối tượng nhúng không được phép");
            }
            if (name.endsWith(".rels")) {
                try (var stream = zipFile.getInputStream(entry)) {
                    byte[] relationshipBytes = stream.readNBytes(MAX_RELATIONSHIP_BYTES + 1);
                    if (relationshipBytes.length > MAX_RELATIONSHIP_BYTES) {
                        throw reject(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED,
                                "Tệp DOCX chứa thông tin liên kết vượt quá giới hạn cho phép");
                    }
                    String relationships = new String(relationshipBytes, StandardCharsets.UTF_8)
                            .toLowerCase(Locale.ROOT);
                    if (relationships.matches("(?s).*targetmode\\s*=\\s*['\"]external['\"].*")) {
                        throw reject(KnowledgeDocumentFilePolicy.MACRO_EMBEDDED_DOCUMENT,
                                "Tệp DOCX chứa liên kết ngoài không được phép");
                    }
                }
            }
        }
    }

    private KnowledgeFileValidationException reject(String code, String message) {
        return new KnowledgeFileValidationException(code, code + ": " + message);
    }
}
