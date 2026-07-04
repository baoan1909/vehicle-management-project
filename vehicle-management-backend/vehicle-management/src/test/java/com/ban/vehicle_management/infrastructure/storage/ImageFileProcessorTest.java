package com.ban.vehicle_management.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageFileProcessorTest {

    @Test
    void shouldAcceptReadablePngImage() throws Exception {
        ImageFileProcessor processor = new ImageFileProcessor(properties(5 * 1024 * 1024, 1280));
        byte[] imageBytes = pngBytes(80, 60);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", imageBytes);

        ImageFileProcessor.PreparedFile preparedFile = processor.prepare(file);

        assertEquals("image/png", preparedFile.contentType());
        assertEquals("png", preparedFile.extension());
        assertEquals(imageBytes.length, preparedFile.bytes().length);
        assertEquals(64, preparedFile.checksumSha256().length());
    }

    @Test
    void shouldResizeWideJpegImage() throws Exception {
        ImageFileProcessor processor = new ImageFileProcessor(properties(5 * 1024 * 1024, 100));
        byte[] imageBytes = jpgBytes(300, 150);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", imageBytes);

        ImageFileProcessor.PreparedFile preparedFile = processor.prepare(file);

        assertEquals("image/jpeg", preparedFile.contentType());
        assertTrue(preparedFile.bytes().length > 0);
    }

    @Test
    void shouldApplyJpegExifOrientation() throws Exception {
        ImageFileProcessor processor = new ImageFileProcessor(properties(5 * 1024 * 1024, 1280));
        byte[] imageBytes = withExifOrientation(jpgBytes(20, 10), 6);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", imageBytes);

        ImageFileProcessor.PreparedFile preparedFile = processor.prepare(file);
        BufferedImage orientedImage = ImageIO.read(new ByteArrayInputStream(preparedFile.bytes()));

        assertEquals("image/jpeg", preparedFile.contentType());
        assertEquals(10, orientedImage.getWidth());
        assertEquals(20, orientedImage.getHeight());
    }

    @Test
    void shouldRejectUnreadableImage() {
        ImageFileProcessor processor = new ImageFileProcessor(properties(5 * 1024 * 1024, 1280));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "not-an-image".getBytes()
        );

        assertThrows(BadRequestException.class, () -> processor.prepare(file));
    }

    private MinioStorageProperties properties(long maxSizeBytes, int maxWidthPixels) {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setImageMaxSizeBytes(maxSizeBytes);
        properties.setImageMaxWidthPixels(maxWidthPixels);
        properties.setImageJpegQuality(0.85);
        return properties;
    }

    private byte[] pngBytes(int width, int height) throws Exception {
        return imageBytes(width, height, "png");
    }

    private byte[] jpgBytes(int width, int height) throws Exception {
        return imageBytes(width, height, "jpg");
    }

    private byte[] imageBytes(int width, int height, String formatName) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, formatName, outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] withExifOrientation(byte[] jpegBytes, int orientation) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(jpegBytes, 0, 2);
        outputStream.writeBytes(exifOrientationSegment(orientation));
        outputStream.write(jpegBytes, 2, jpegBytes.length - 2);
        return outputStream.toByteArray();
    }

    private byte[] exifOrientationSegment(int orientation) {
        byte[] payload = new byte[]{
                'E', 'x', 'i', 'f', 0, 0,
                'I', 'I', 42, 0, 8, 0, 0, 0,
                1, 0,
                0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientation, 0, 0, 0,
                0, 0, 0, 0
        };
        int length = payload.length + 2;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(0xFF);
        outputStream.write(0xE1);
        outputStream.write((length >>> 8) & 0xFF);
        outputStream.write(length & 0xFF);
        outputStream.writeBytes(payload);
        return outputStream.toByteArray();
    }
}
