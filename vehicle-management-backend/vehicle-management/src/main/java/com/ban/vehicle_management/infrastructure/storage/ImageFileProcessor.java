package com.ban.vehicle_management.infrastructure.storage;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileProcessor {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MinioStorageProperties properties;

    public ImageFileProcessor(MinioStorageProperties properties) {
        this.properties = properties;
    }

    public PreparedFile prepare(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file must not be empty");
        }
        if (file.getSize() > properties.getImageMaxSizeBytes()) {
            throw new BadRequestException("file must not exceed " + properties.getImageMaxSizeBytes() + " bytes");
        }

        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String extension = resolveExtension(originalFilename, file.getContentType());
        String contentType = resolveContentType(file.getContentType(), extension);
        byte[] originalBytes = readBytes(file);

        byte[] preparedBytes = isWebp(extension)
                ? validateWebp(originalBytes)
                : validateAndResizeImage(originalBytes, extension, contentType);
        return new PreparedFile(
                preparedBytes,
                originalFilename,
                contentType,
                extension,
                checksumSha256(preparedBytes)
        );
    }

    private String normalizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        String normalizedFilename = originalFilename.replace('\\', '/');
        int lastSlashIndex = normalizedFilename.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalizedFilename.substring(lastSlashIndex + 1) : normalizedFilename;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String extension = null;
        int lastDotIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        }
        if (extension == null || extension.isBlank()) {
            extension = extensionFromContentType(contentType);
        }
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("file extension must be jpg, jpeg, png, or webp");
        }
        return extension;
    }

    private String resolveContentType(String contentType, String extension) {
        String normalizedContentType = contentType == null ? null : contentType.toLowerCase(Locale.ROOT).trim();
        if ("image/jpg".equals(normalizedContentType)) {
            normalizedContentType = "image/jpeg";
        }
        if (normalizedContentType == null || normalizedContentType.isBlank()) {
            normalizedContentType = contentTypeFromExtension(extension);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new BadRequestException("file content type must be image/jpeg, image/png, or image/webp");
        }
        if (!isExtensionCompatibleWithContentType(extension, normalizedContentType)) {
            throw new BadRequestException("file extension and content type do not match");
        }
        return normalizedContentType;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("file could not be read");
        }
    }

    private byte[] validateWebp(byte[] bytes) {
        if (bytes.length < 12
                || bytes[0] != 'R'
                || bytes[1] != 'I'
                || bytes[2] != 'F'
                || bytes[3] != 'F'
                || bytes[8] != 'W'
                || bytes[9] != 'E'
                || bytes[10] != 'B'
                || bytes[11] != 'P') {
            throw new BadRequestException("file must be a readable image");
        }
        return bytes;
    }

    private byte[] validateAndResizeImage(byte[] bytes, String extension, String contentType) {
        BufferedImage image = readImage(bytes);
        int orientation = isJpeg(extension) ? readExifOrientation(bytes) : 1;
        BufferedImage orientedImage = applyExifOrientation(image, orientation);
        int maxWidthPixels = properties.getImageMaxWidthPixels();
        if (orientation == 1 && (maxWidthPixels <= 0 || orientedImage.getWidth() <= maxWidthPixels)) {
            return bytes;
        }

        BufferedImage preparedImage = orientedImage;
        if (maxWidthPixels > 0 && orientedImage.getWidth() > maxWidthPixels) {
            double scale = (double) maxWidthPixels / orientedImage.getWidth();
            int targetHeight = Math.max(1, (int) Math.round(orientedImage.getHeight() * scale));
            preparedImage = resize(orientedImage, maxWidthPixels, targetHeight, "image/png".equals(contentType));
        }

        return isJpeg(extension)
                ? writeJpeg(preparedImage, properties.getImageJpegQuality())
                : writeImage(preparedImage, extension);
    }

    private BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BadRequestException("file must be a readable image");
            }
            return image;
        } catch (IOException exception) {
            throw new BadRequestException("file must be a readable image");
        }
    }

    private BufferedImage resize(BufferedImage sourceImage, int targetWidth, int targetHeight, boolean keepAlpha) {
        int imageType = keepAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    private int readExifOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory == null || !directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return 1;
            }
            return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (Exception exception) {
            return 1;
        }
    }

    private BufferedImage applyExifOrientation(BufferedImage sourceImage, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return sourceImage;
        }

        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        boolean swapsDimensions = orientation >= 5;
        int targetWidth = swapsDimensions ? sourceHeight : sourceWidth;
        int targetHeight = swapsDimensions ? sourceWidth : sourceHeight;
        int imageType = sourceImage.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, imageType);

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                int rgb = sourceImage.getRGB(x, y);
                switch (orientation) {
                    case 2 -> targetImage.setRGB(sourceWidth - 1 - x, y, rgb);
                    case 3 -> targetImage.setRGB(sourceWidth - 1 - x, sourceHeight - 1 - y, rgb);
                    case 4 -> targetImage.setRGB(x, sourceHeight - 1 - y, rgb);
                    case 5 -> targetImage.setRGB(y, x, rgb);
                    case 6 -> targetImage.setRGB(sourceHeight - 1 - y, x, rgb);
                    case 7 -> targetImage.setRGB(sourceHeight - 1 - y, sourceWidth - 1 - x, rgb);
                    case 8 -> targetImage.setRGB(y, sourceWidth - 1 - x, rgb);
                    default -> targetImage.setRGB(x, y, rgb);
                }
            }
        }
        return targetImage;
    }

    private byte[] writeJpeg(BufferedImage image, double quality) {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG image writer is available");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality((float) Math.max(0.0, Math.min(1.0, quality)));
            }
            writer.write(null, new IIOImage(rgbImage, null, null), writeParam);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resize image", exception);
        } finally {
            writer.dispose();
        }
    }

    private byte[] writeImage(BufferedImage image, String extension) {
        String formatName = "jpg".equals(extension) ? "jpeg" : extension;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, formatName, outputStream)) {
                throw new IllegalStateException("No image writer is available for " + extension);
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resize image", exception);
        }
    }

    private String checksumSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT).trim()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> null;
        };
    }

    private String contentTypeFromExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> null;
        };
    }

    private boolean isExtensionCompatibleWithContentType(String extension, String contentType) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg".equals(contentType);
            case "png" -> "image/png".equals(contentType);
            case "webp" -> "image/webp".equals(contentType);
            default -> false;
        };
    }

    private boolean isWebp(String extension) {
        return "webp".equals(extension);
    }

    private boolean isJpeg(String extension) {
        return "jpg".equals(extension) || "jpeg".equals(extension);
    }

    public record PreparedFile(
            byte[] bytes,
            String originalFilename,
            String contentType,
            String extension,
            String checksumSha256
    ) {
    }
}
