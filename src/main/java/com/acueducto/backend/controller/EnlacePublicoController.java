package com.acueducto.backend.controller;

import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.service.DocumentoService;
import com.acueducto.backend.service.EnlacePublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Descarga publica de facturas y recibos mediante un enlace temporal (sin iniciar sesion).
 * Es el enlace que genera Admin/Tesorero con POST .../public-link y se envia por WhatsApp.
 * Si el enlace expiro o no existe, el backend responde el mensaje "Enlace expirado o no
 * disponible. Este enlace dejo de estar disponible."
 */
@Tag(name = "19. Enlaces publicos de documentos", description = "Descarga publica (sin login) de facturas y recibos en PDF mediante enlace temporal compartido")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class EnlacePublicoController {

    private final EnlacePublicoService enlacePublicoService;
    private final DocumentoService documentoService;

    @Operation(summary = "Descargar factura publica via enlace temporal",
            description = "Sin iniciar sesion. Valida que el token exista, no este vencido y que la factura exista y no este anulada. Entrega el PDF listo para descargar.")
    @GetMapping(value = "/facturas/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarFacturaPublica(@PathVariable String token) {
        Factura factura = enlacePublicoService.obtenerFacturaPublica(token);
        byte[] pdf = documentoService.generarFacturaPdf(factura);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumeroFactura() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Descargar recibo publico via enlace temporal",
            description = "Sin iniciar sesion. Valida que el token exista, no este vencido y que el recibo exista y no este anulado. Entrega el PDF listo para descargar.")
    @GetMapping(value = "/recibos/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReciboPublico(@PathVariable String token) {
        Recibo recibo = enlacePublicoService.obtenerReciboPublico(token);
        byte[] pdf = documentoService.generarReciboPdf(recibo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + recibo.getNumeroRecibo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}