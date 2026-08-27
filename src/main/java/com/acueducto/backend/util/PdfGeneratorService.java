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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PdfGeneratorService.class);

    /** Convierte el HTML ya renderizado a bytes PDF. */
    public byte[] generarPdfDesdeHtml(String html) {
        try {
            String limpio = html;
            if (limpio != null) {
                // Elimina BOM y cualquier caracter antes del primer '<' (causa "Content is not allowed in prolog")
                limpio = limpio.replaceFirst("^\uFEFF", "");
                limpio = limpio.replaceFirst("^\uFEFF", "");
                limpio = limpio.trim();
                limpio = limpio.replaceAll("(?s)^\\s*<\\?xml[^?]*\\?>\\s*", "");
                limpio = limpio.replaceAll("(?si)<!DOCTYPE[^>]*>\\s*", "");
                limpio = limpio.replaceFirst("^\uFEFF", "");
                // Fallback: si aún queda algo antes de '<', lo elimina (ej. '?' por BOM mal decodificado)
                int idx = limpio.indexOf('<');
                if (idx > 0) {
                    limpio = limpio.substring(idx);
                }
                limpio = limpio.trim();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(limpio);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF, html length {}: {}", html != null ? html.length() : 0, e.getMessage(), e);
            // Incluye html parcial para debug en Render
            String snippet = html != null && html.length() > 2000 ? html.substring(0, 2000) : html;
            log.error("HTML snippet: {}", snippet);
            throw new RuntimeException("No fue posible generar el PDF: " + e.getMessage(), e);
        }
    }

    public byte[] generarPdf(String nombrePlantilla, Context context) {
        String html = renderizarHtml(nombrePlantilla, context);
        return generarPdfDesdeHtml(html);
    }
}
