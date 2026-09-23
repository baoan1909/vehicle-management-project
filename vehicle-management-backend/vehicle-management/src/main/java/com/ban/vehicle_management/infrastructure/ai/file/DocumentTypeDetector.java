package com.ban.vehicle_management.infrastructure.ai.file;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Locale;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Component;

/**
 * Decides the real content type of an uploaded file from its bytes, never from the
 * client-declared Content-Type. The declared extension selects the expected family;
 * this detector confirms that the bytes actually belong to that family.
 */
@Component
public class DocumentTypeDetector {

    public enum DocumentType {
        PDF,
        DOCX_ZIP,
        HTML,
        PLAIN_TEXT,
        UNKNOWN
    }

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] ZIP_MAGIC = {'P', 'K'};

    public DocumentType detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return DocumentType.UNKNOWN;
        }
        if (startsWith(bytes, PDF_MAGIC)) {
            return DocumentType.PDF;
        }
        if (startsWith(bytes, ZIP_MAGIC)) {
            return DocumentType.DOCX_ZIP;
        }
        if (looksLikeHtml(bytes)) {
            return DocumentType.HTML;
        }
        if (isUtf8Text(bytes)) {
            return DocumentType.PLAIN_TEXT;
        }
        return DocumentType.UNKNOWN;
    }

    public boolean isZip(byte[] bytes) {
        return startsWith(bytes, ZIP_MAGIC);
    }

    public boolean isPdf(byte[] bytes) {
        return startsWith(bytes, PDF_MAGIC);
    }

    /** True when the bytes decode cleanly as UTF-8 and contain no NUL control bytes. */
    public boolean isUtf8Text(byte[] bytes) {
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    public boolean zipHasEntry(byte[] bytes, String entryName) {
        try (ZipFile zipFile = openZip(bytes)) {
            return zipFile.getEntry(entryName) != null;
        } catch (Exception exception) {
            return false;
        }
    }

    public ZipFile openZip(byte[] bytes) throws Exception {
        return ZipFile.builder()
                .setSeekableByteChannel(new org.apache.commons.compress.utils.SeekableInMemoryByteChannel(bytes))
                .get();
    }

    /**
     * True when any entry reports encryption or the OOXML encryption marker exists.
     */
    public boolean zipIsEncrypted(byte[] bytes) {
        try (ZipFile zipFile = openZip(bytes)) {
            java.util.Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getGeneralPurposeBit().usesEncryption()) {
                    return true;
                }
            }
        } catch (Exception exception) {
            return true;
        }
        return false;
    }

    private boolean looksLikeHtml(byte[] bytes) {
        int sampleLimit = Math.min(bytes.length, 2048);
        String sample = new String(bytes, 0, sampleLimit, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        String trimmed = sample.stripLeading();
        return trimmed.startsWith("<!doctype html")
                || trimmed.startsWith("<html")
                || sample.contains("<body")
                || sample.contains("<p>")
                || sample.contains("<div");
    }

    private boolean startsWith(byte[] bytes, byte[] magic) {
        if (bytes.length < magic.length) {
            return false;
        }
        for (int index = 0; index < magic.length; index++) {
            if (bytes[index] != magic[index]) {
                return false;
            }
        }
        return true;
    }
}
