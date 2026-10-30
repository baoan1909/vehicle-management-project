package com.ban.vehicle_management.infrastructure.qrcode;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionQrImagePortOut;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionQrImageAdapter implements SubscriptionQrImagePortOut {

    private static final int IMAGE_SIZE = 320;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final String DATA_URL_PREFIX = "data:image/png;base64,";

    @Override
    public String encodePngDataUrl(String payload) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new QRCodeWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    IMAGE_SIZE,
                    IMAGE_SIZE,
                    hints
            );
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return DATA_URL_PREFIX + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Unable to encode subscription QR image", exception);
        }
    }

    @Override
    public Optional<String> decode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (input == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    return Optional.empty();
                }
                BufferedImage image = reader.read(0);
                BinaryBitmap bitmap = new BinaryBitmap(
                        new HybridBinarizer(new BufferedImageLuminanceSource(image)));
                return Optional.of(new MultiFormatReader().decode(bitmap).getText());
            } finally {
                reader.dispose();
            }
        } catch (IOException | com.google.zxing.NotFoundException exception) {
            return Optional.empty();
        }
    }
}
