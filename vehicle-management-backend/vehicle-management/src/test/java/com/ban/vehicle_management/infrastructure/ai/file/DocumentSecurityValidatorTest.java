package com.ban.vehicle_management.infrastructure.ai.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.shared.exception.KnowledgeFileValidationException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class DocumentSecurityValidatorTest {

    private static final long MAX_BYTES = 1024 * 1024;

    private final DocumentSecurityValidator validator =
            new DocumentSecurityValidator(new DocumentTypeDetector());

    @Test
    void validateShouldAcceptAWellFormedPdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        assertDoesNotThrow(() -> validator.validate(pdf, "pdf", MAX_BYTES));
    }

    @Test
    void validateShouldRejectFakePdfMagicBytes() {
        KnowledgeFileValidationException exception = assertThrows(
                KnowledgeFileValidationException.class,
                () -> validator.validate("%PDF-not-a-real-pdf".getBytes(StandardCharsets.UTF_8),
                        "pdf", MAX_BYTES));

        assertEquals(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE, exception.code());
    }

    @Test
    void validateShouldRejectDocxWithExternalRelationship() throws Exception {
        byte[] docx = docxWithExternalRelationship();

        KnowledgeFileValidationException exception = assertThrows(
                KnowledgeFileValidationException.class,
                () -> validator.validate(docx, "docx", MAX_BYTES));

        assertEquals(KnowledgeDocumentFilePolicy.MACRO_EMBEDDED_DOCUMENT, exception.code());
    }

    @Test
    void validateShouldRejectInvalidUtf8AfterTheInitialSample() {
        byte[] payload = new byte[9_000];
        Arrays.fill(payload, (byte) 'a');
        payload[8_500] = (byte) 0xC3;
        payload[8_501] = (byte) 0x28;

        KnowledgeFileValidationException exception = assertThrows(
                KnowledgeFileValidationException.class,
                () -> validator.validate(payload, "txt", MAX_BYTES));

        assertEquals(KnowledgeDocumentFilePolicy.INVALID_SIGNATURE, exception.code());
    }

    @Test
    void validateShouldRejectOversizedDocxRelationshipBeforeLoadingItAll() throws Exception {
        byte[] docx;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeEntry(zip, "[Content_Types].xml", "<Types/>");
            writeEntry(zip, "word/document.xml", "<w:document/>");
            writeEntry(zip, "word/_rels/document.xml.rels", "a".repeat(1024 * 1024 + 1));
            zip.finish();
            docx = output.toByteArray();
        }

        KnowledgeFileValidationException exception = assertThrows(
                KnowledgeFileValidationException.class,
                () -> validator.validate(docx, "docx", MAX_BYTES));

        assertEquals(KnowledgeDocumentFilePolicy.ZIP_BOMB_DETECTED, exception.code());
    }

    private byte[] docxWithExternalRelationship() throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeEntry(zip, "[Content_Types].xml", "<Types/>");
            writeEntry(zip, "word/document.xml", "<w:document/>");
            writeEntry(zip, "word/_rels/document.xml.rels",
                    "<Relationships><Relationship TargetMode=\"External\" "
                            + "Target=\"https://example.invalid/payload\"/></Relationships>");
            zip.finish();
            return output.toByteArray();
        }
    }

    private void writeEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
