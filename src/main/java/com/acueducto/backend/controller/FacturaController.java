package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.ConceptoFacturaRequest;
import com.acueducto.backend.dto.request.GenerarFacturacionMesRequest;
import com.acueducto.backend.dto.response.FacturaResponse;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.DocumentoService;
import com.acueducto.backend.service.FacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "07. Facturacion", description = "Generacion y consulta de facturas, version HTML/PDF y codigo QR (Modulo 7)")
@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final DocumentoService documentoService;

    @Operation(summary = "Generar facturacion del mes", description = "Procesa todas las lecturas pendientes del periodo y genera una factura por cada una (6.14).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/generar-mes")
    public ResponseEntity<List<FacturaResponse>> generarFacturacionMes(@Valid @RequestBody GenerarFacturacionMesRequest request) {
        return ResponseEntity.ok(facturaService.generarFacturacionMes(request.mesContableId()));
    }

    @Operation(summary = "Agregar concepto adicional a una factura")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/conceptos")
    public ResponseEntity<FacturaResponse> agregarConcepto(@Valid @RequestBody ConceptoFacturaRequest request) {
        return ResponseEntity.ok(facturaService.agregarConcepto(request));
    }

    @Operation(summary = "Anular factura")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/{id}/anular")
    public ResponseEntity<FacturaResponse> anular(@PathVariable Long id, @RequestParam String motivo) {
        return ResponseEntity.ok(facturaService.anular(id, motivo));
    }

    @Operation(summary = "Ver detalle de una factura")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponse> obtener(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        verificarAccesoAsociado(facturaService.obtenerEntidad(id), principal);
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    @Operation(summary = "Listar facturas de un asociado")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#asociadoId))")
    @GetMapping("/asociado/{asociadoId}")
    public ResponseEntity<Page<FacturaResponse>> listarPorAsociado(@PathVariable Long asociadoId, Pageable pageable) {
        return ResponseEntity.ok(facturaService.listarPorAsociado(asociadoId, pageable));
    }

    @Operation(summary = "Listar facturas por estado")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping
    public ResponseEntity<Page<FacturaResponse>> listarPorEstado(@RequestParam EstadoFactura estado, Pageable pageable) {
        return ResponseEntity.ok(facturaService.listarPorEstado(estado, pageable));
    }

    @Operation(summary = "Historial completo de facturas", description = "Todas las facturas del sistema, sin filtrar por estado ni asociado. Admite orden y paginacion (ej: ?sort=fechaEmision,desc).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/todas")
    public ResponseEntity<Page<FacturaResponse>> listarTodas(Pageable pageable) {
        return ResponseEntity.ok(facturaService.listarTodas(pageable));
    }

    @Operation(summary = "Ver factura en HTML", description = "Version oficial en linea, identica a la version PDF (7.9).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verHtml(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Factura factura = facturaService.obtenerEntidad(id);
        verificarAccesoAsociado(factura, principal);
        return ResponseEntity.ok(documentoService.renderizarFacturaHtml(factura));
    }

    @Operation(summary = "Descargar factura en PDF", description = "Mismo diseno que la version HTML (7.10).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Factura factura = facturaService.obtenerEntidad(id);
        verificarAccesoAsociado(factura, principal);
        byte[] pdf = documentoService.generarFacturaPdf(factura);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumeroFactura() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Consultar factura via codigo QR",
            description = "Endpoint publico usado al escanear el QR (7.11). El Tesorero puede continuar hacia el registro del pago; el Asociado solo consulta.")
    @GetMapping("/qr/{numeroFactura}")
    public ResponseEntity<FacturaResponse> consultarPorQr(@PathVariable String numeroFactura) {
        Factura factura = facturaService.obtenerPorNumero(numeroFactura);
        return ResponseEntity.ok(FacturaResponse.fromEntity(factura));
    }

    private void verificarAccesoAsociado(Factura factura, UserPrincipal principal) {
        if (principal == null) return;
        boolean esAsociado = principal.getUsuario().getRol() == com.acueducto.backend.entity.enums.Rol.ASOCIADO;
        if (esAsociado) {
            Long asociadoIdUsuario = principal.getUsuario().getAsociado() != null ? principal.getUsuario().getAsociado().getId() : null;
            if (asociadoIdUsuario == null || !asociadoIdUsuario.equals(factura.getAsociado().getId())) {
                throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                        "Solo puede consultar informacion relacionada con su propia cuenta.");
            }
        }
    }
}
