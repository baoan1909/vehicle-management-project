package com.ban.vehicle_management.infrastructure.qrcode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SubscriptionQrImageAdapterTest {

    @Test
    void shouldEncodeQrImageThatCanBeDecoded() throws Exception {
        String payload = "COPARKING:SUBSCRIPTION:1:0123456789012345678901234567890123456789012";
        SubscriptionQrImageAdapter adapter = new SubscriptionQrImageAdapter();
        String dataUrl = adapter.encodePngDataUrl(payload);
        byte[] imageBytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        var image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        assertEquals(payload, new MultiFormatReader().decode(bitmap).getText());
        assertEquals(payload, adapter.decode(imageBytes).orElseThrow());
    }
}
