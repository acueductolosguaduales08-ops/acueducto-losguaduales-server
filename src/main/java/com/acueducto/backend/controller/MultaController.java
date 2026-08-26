package com.acueducto.backend.controller;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.MultaRepository;
import com.acueducto.backend.service.DocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> verMultaHtml(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        return ResponseEntity.ok(documentoService.renderizarMultaHtml(multa));
    }

    @Operation(summary = "Descargar multa en PDF", description = "Genera y descarga el comprobante de multa en formato PDF.")
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> descargarMultaPdf(@PathVariable Long id) {
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));
        byte[] pdf = documentoService.generarMultaPdf(multa);
        String nombreArchivo = "Multa-" + String.format("%06d", id) + ".pdf";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
