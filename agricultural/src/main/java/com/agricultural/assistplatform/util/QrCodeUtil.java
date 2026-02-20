package com.agricultural.assistplatform.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 溯源二维码生成（ZXing），返回 Data URL 便于前端展示或存入 DB
 */
@Slf4j
@Component
public class QrCodeUtil {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;

    public String generateAndSave(String content, String key) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.warn("QR code generate fail: {}", e.getMessage());
            return null;
        }
    }
}
