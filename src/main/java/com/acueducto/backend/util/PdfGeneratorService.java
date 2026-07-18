package com.acueducto.backend.util;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Convierte plantillas Thymeleaf (facturas y recibos) a PDF usando Flying Saucer + OpenPDF,
 * garantizando que el documento descargado sea visualmente identico a la version HTML (7.10 / 8.13).
 */
@Service
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    public PdfGeneratorService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /** Renderiza una plantilla Thymeleaf a HTML puro (usado tambien para la vista previa en linea). */
    public String renderizarHtml(String nombrePlantilla, Context context) {
        return templateEngine.process(nombrePlantilla, context);
    }

    /** Convierte el HTML ya renderizado a bytes PDF. */
    public byte[] generarPdfDesdeHtml(String html) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No fue posible generar el PDF", e);
        }
    }

    public byte[] generarPdf(String nombrePlantilla, Context context) {
        String html = renderizarHtml(nombrePlantilla, context);
        return generarPdfDesdeHtml(html);
    }
}
