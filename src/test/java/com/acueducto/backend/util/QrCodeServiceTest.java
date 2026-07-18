package com.acueducto.backend.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    @Test
    void generarQrFormularioDebeApuntarALaRutaDeEncuestasConCodigo() throws Exception {
        QrCodeService qrCodeService = new QrCodeService();
        ReflectionTestUtils.setField(qrCodeService, "baseUrl", "https://acueducto-los-guaduales.pages.dev");

        String qrDataUri = qrCodeService.generarQrFormulario("FRM-000001");

        assertTrue(qrDataUri.startsWith("data:image/png;base64,"));
        assertEquals(
            "https://acueducto-los-guaduales.pages.dev/encuestas?codigo=FRM-000001",
            decodificarContenidoQr(qrDataUri)
        );
    }

    private String decodificarContenidoQr(String qrDataUri) throws Exception {
        String[] partes = qrDataUri.split(",", 2);
        byte[] imagenBytes = Base64.getDecoder().decode(partes[1]);
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(imagenBytes));

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        Result resultado = new MultiFormatReader().decode(bitmap);
        return resultado.getText();
    }
}
