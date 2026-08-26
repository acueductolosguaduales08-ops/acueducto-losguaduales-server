package com.acueducto.backend.controller;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.MultaRepository;
import com.acueducto.backend.service.DocumentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "08b. Multa Documento", description = "Vista previa HTML y descarga PDF de comprobantes de multa")
@RestController
@RequestMapping("/api/v1/multas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MultaController {

    private final MultaRepository multaRepository;
    private final DocumentoService documentoService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Ver multa en HTML", description = "Renderiza la multa en formato HTML para vista previa en navegador.")
    @GetMapping(value = "/{id}/html", produces = "text/html")
    @Transactional(readOnly = true)
    public ResponseEntity<?> verMultaHtml(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        try {
            String html = documentoService.renderizarMultaHtml(multa);
            return ResponseEntity.ok(html);
        } catch (Exception ex) {
            log.error("Error renderizando HTML de multa {}: {}", id, ex.getMessage(), ex);
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "error", ex.getClass().getSimpleName(),
                        "mensaje", ex.getMessage() != null ? ex.getMessage() : "Error desconocido"
                ));
                return ResponseEntity.internalServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Error renderizando multa: " + ex.getMessage());
            }
        }
    }

    @Operation(summary = "Descargar multa en PDF", description = "Genera y descarga el comprobante de multa en formato PDF.")
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<?> descargarMultaPdf(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        try {
            byte[] pdf = documentoService.generarMultaPdf(multa);
            String nombreArchivo = "Multa-" + String.format("%06d", id) + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception ex) {
            log.error("Error generando PDF de multa {}: {}", id, ex.getMessage(), ex);
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "error", ex.getClass().getSimpleName(),
                        "mensaje", ex.getMessage() != null ? ex.getMessage() : "Error desconocido"
                ));
                return ResponseEntity.internalServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json);
            } catch (Exception jsonEx) {
                return ResponseEntity.internalServerError()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Error generando PDF de multa: " + ex.getMessage());
            }
        }
    }
}
