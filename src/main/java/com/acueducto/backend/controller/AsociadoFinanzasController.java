package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.LecturaResponse;
import com.acueducto.backend.dto.response.MovimientoTesoreriaResponse;
import com.acueducto.backend.dto.response.MultaResponse;
import com.acueducto.backend.dto.response.ReciboResponse;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.DocumentoService;
import com.acueducto.backend.service.LecturaService;
import com.acueducto.backend.service.TesoreriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints financieros del asociado: sus propias multas, movimientos y recibos.
 * Cada endpoint verifica que el asociado autenticado solo acceda a sus propios datos.
 */
@Tag(name = "10. Mis Finanzas", description = "Multas, movimientos y recibos propios del asociado")
@RestController
@RequestMapping("/api/v1/mis-finanzas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ASOCIADO')")
public class AsociadoFinanzasController {

    private final TesoreriaService tesoreriaService;
    private final DocumentoService documentoService;
    private final LecturaService lecturaService;

    @Operation(summary = "Mi consumo de agua", description = "Historial de lecturas y consumo del asociado autenticado.")
    @GetMapping("/consumo")
    public ResponseEntity<List<LecturaResponse>> listarMiConsumo(@AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        return ResponseEntity.ok(lecturaService.historialPorAsociado(asociadoId));
    }

    @Operation(summary = "Mis multas")
    @GetMapping("/multas")
    public ResponseEntity<List<MultaResponse>> listarMisMultas(@AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        return ResponseEntity.ok(tesoreriaService.listarMultasPorAsociado(asociadoId));
    }

    @Operation(summary = "Mis movimientos (entradas y salidas)")
    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoTesoreriaResponse>> listarMisMovimientos(@AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        return ResponseEntity.ok(tesoreriaService.listarMovimientosPorAsociado(asociadoId));
    }

    @Operation(summary = "Mis recibos")
    @GetMapping("/recibos")
    public ResponseEntity<Page<ReciboResponse>> listarMisRecibos(
            @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        return ResponseEntity.ok(tesoreriaService.listarRecibosPorAsociado(asociadoId, pageable));
    }

    @Operation(summary = "Obtener recibo de una multa pagada")
    @GetMapping("/multas/{multaId}/recibo")
    public ResponseEntity<ReciboResponse> reciboDeMulta(
            @PathVariable Long multaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        Recibo recibo = tesoreriaService.obtenerReciboPorMulta(multaId);
        if (!recibo.getAsociado().getId().equals(asociadoId)) {
            throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                    "Solo puede consultar informacion relacionada con su propia cuenta.");
        }
        return ResponseEntity.ok(ReciboResponse.fromEntity(recibo));
    }

    @Operation(summary = "Ver recibo en HTML")
    @GetMapping(value = "/recibos/{numeroRecibo}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verHtml(@PathVariable String numeroRecibo, @AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        Recibo recibo = tesoreriaService.obtenerReciboPorNumero(numeroRecibo);
        if (!recibo.getAsociado().getId().equals(asociadoId)) {
            throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                    "Solo puede consultar informacion relacionada con su propia cuenta.");
        }
        return ResponseEntity.ok(documentoService.renderizarReciboHtml(recibo));
    }

    @Operation(summary = "Descargar recibo en PDF")
    @GetMapping(value = "/recibos/{numeroRecibo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String numeroRecibo, @AuthenticationPrincipal UserPrincipal principal) {
        Long asociadoId = principal.getUsuario().getAsociado().getId();
        Recibo recibo = tesoreriaService.obtenerReciboPorNumero(numeroRecibo);
        if (!recibo.getAsociado().getId().equals(asociadoId)) {
            throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                    "Solo puede consultar informacion relacionada con su propia cuenta.");
        }
        byte[] pdf = documentoService.generarReciboPdf(recibo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + recibo.getNumeroRecibo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
