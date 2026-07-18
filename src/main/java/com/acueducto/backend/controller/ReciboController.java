package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.ReciboResponse;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.DocumentoService;
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

@Tag(name = "09. Recibos", description = "Consulta de recibos de pago, version HTML/PDF y codigo QR (8.12 / 8.13)")
@RestController
@RequestMapping("/api/v1/recibos")
@RequiredArgsConstructor
public class ReciboController {

    private final TesoreriaService tesoreriaService;
    private final DocumentoService documentoService;

    @Operation(summary = "Listar recibos de un asociado")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#asociadoId))")
    @GetMapping("/asociado/{asociadoId}")
    public ResponseEntity<Page<ReciboResponse>> listarPorAsociado(@PathVariable Long asociadoId, Pageable pageable) {
        return ResponseEntity.ok(tesoreriaService.listarRecibosPorAsociado(asociadoId, pageable));
    }

    @Operation(summary = "Ver recibo en HTML")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping(value = "/{numeroRecibo}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verHtml(@PathVariable String numeroRecibo, @AuthenticationPrincipal UserPrincipal principal) {
        Recibo recibo = tesoreriaService.obtenerReciboPorNumero(numeroRecibo);
        verificarAcceso(recibo, principal);
        return ResponseEntity.ok(documentoService.renderizarReciboHtml(recibo));
    }

    @Operation(summary = "Descargar recibo en PDF")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping(value = "/{numeroRecibo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String numeroRecibo, @AuthenticationPrincipal UserPrincipal principal) {
        Recibo recibo = tesoreriaService.obtenerReciboPorNumero(numeroRecibo);
        verificarAcceso(recibo, principal);
        byte[] pdf = documentoService.generarReciboPdf(recibo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + recibo.getNumeroRecibo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Consultar recibo via codigo QR")
    @GetMapping("/qr/{numeroRecibo}")
    public ResponseEntity<ReciboResponse> consultarPorQr(@PathVariable String numeroRecibo) {
        Recibo recibo = tesoreriaService.obtenerReciboPorNumero(numeroRecibo);
        return ResponseEntity.ok(ReciboResponse.fromEntity(recibo));
    }

    private void verificarAcceso(Recibo recibo, UserPrincipal principal) {
        if (principal == null) return;
        boolean esAsociado = principal.getUsuario().getRol() == com.acueducto.backend.entity.enums.Rol.ASOCIADO;
        if (esAsociado) {
            Long asociadoIdUsuario = principal.getUsuario().getAsociado() != null ? principal.getUsuario().getAsociado().getId() : null;
            if (asociadoIdUsuario == null || !asociadoIdUsuario.equals(recibo.getAsociado().getId())) {
                throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                        "Solo puede consultar informacion relacionada con su propia cuenta.");
            }
        }
    }
}
