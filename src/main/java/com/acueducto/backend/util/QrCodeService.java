package com.acueducto.backend.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Genera codigos QR para facturas, recibos y formularios (7.11 / 12.14).
 * Cada QR codifica una URL segura hacia el recurso correspondiente. Mientras se define
 * el dominio definitivo del frontend se usa la URL base configurada en app.qr.base-url
 * (ver seccion 12.14 del documento funcional: "por el momento no hay url... dejarlas asi").
 */
@Service
public class QrCodeService {

    @Value("${app.qr.base-url}")
    private String baseUrl;

    private static final int TAMANO = 300;

    public String generarQrFactura(String numeroFactura) {
        return generarQrBase64(baseUrl + "/factura/" + numeroFactura);
    }

    public String generarQrRecibo(String numeroRecibo) {
        return generarQrBase64(baseUrl + "/recibo/" + numeroRecibo);
    }

    public String generarQrFormulario(String codigoFormulario) {
        return generarQrBase64(baseUrl + "/formulario/" + codigoFormulario);
    }

    /** Genera un QR a partir de cualquier contenido y lo retorna como PNG codificado en Base64 (data URI). */
    public String generarQrBase64(String contenido) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contenido, BarcodeFormat.QR_CODE, TAMANO, TAMANO);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            throw new RuntimeException("No fue posible generar el codigo QR", e);
        }
    }
}
