package com.acueducto.backend.controller;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.MultaRepository;
import com.acueducto.backend.service.DocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "08b. Multa Documento", description = "Vista previa HTML y descarga PDF de comprobantes de multa")
@RestController
@RequestMapping("/api/v1/multas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MultaController {

    private final MultaRepository multaRepository;
    private final DocumentoService documentoService;

    @Operation(summary = "Ver multa en HTML", description = "Renderiza la multa en formato HTML para vista previa en navegador.")
    @GetMapping(value = "/{id}/html", produces = "text/html")
    @Transactional(readOnly = true)
    public ResponseEntity<String> verMultaHtml(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        try {
            String html = documentoService.renderizarMultaHtml(multa);
            return ResponseEntity.ok(html);
        } catch (Exception ex) {
            log.error("Error renderizando HTML de multa {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.internalServerError()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + ex.getClass().getSimpleName() + "\",\"mensaje\":\"" + ex.getMessage() + "\"}");
        }
    }

    @Operation(summary = "Descargar multa en PDF", description = "Genera y descarga el comprobante de multa en formato PDF.")
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarMultaPdf(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        try {
            byte[] pdf = documentoService.generarMultaPdf(multa);
            String nombreArchivo = "Multa-" + String.format("%06d", id) + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception ex) {
            log.error("Error generando PDF de multa {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.internalServerError()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"" + ex.getClass().getSimpleName() + "\",\"mensaje\":\"" + ex.getMessage() + "\"}").getBytes());
        }
    }
}
