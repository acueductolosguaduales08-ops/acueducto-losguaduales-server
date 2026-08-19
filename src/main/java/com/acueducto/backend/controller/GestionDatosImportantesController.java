package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.ConfirmarPasswordRequest;
import com.acueducto.backend.service.GestionDatosImportantesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Modulo "Gestion de datos importantes" (5): eliminacion DEFINITIVA e irreversible. Exclusivo
 * del Administrador. Cada borrado pide reconfirmar la contrasena en el body, aunque ya haya
 * sesion iniciada, y queda registrado en auditoria. El frontend debe mostrar una confirmacion
 * explicita antes de llamar a estos endpoints (por ejemplo: "esto se va a perder para
 * siempre, ¿continuar?"), ver la documentacion.
 */
@Tag(name = "12. Gestion de datos importantes", description = "Eliminacion definitiva e irreversible. Solo Administrador, pide reconfirmar contrasena.")
@RestController
@RequestMapping("/api/v1/datos-importantes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class GestionDatosImportantesController {

    private final GestionDatosImportantesService service;

    @Operation(summary = "Eliminar un formulario definitivamente", description = "Borra en cascada sus preguntas y las respuestas ya recibidas.")
    @DeleteMapping("/formularios/{id}")
    public ResponseEntity<Void> eliminarFormulario(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarFormulario(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una factura definitivamente", description = "Bloqueada si la factura tiene pagos registrados.")
    @DeleteMapping("/facturas/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarFactura(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un recibo definitivamente")
    @DeleteMapping("/recibos/{id}")
    public ResponseEntity<Void> eliminarRecibo(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarRecibo(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un asociado definitivamente", description = "Bloqueado si tiene historial (facturas, pagos o lecturas); use archivar en ese caso.")
    @DeleteMapping("/asociados/{id}")
    public ResponseEntity<Void> eliminarAsociado(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarAsociado(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una cuenta definitivamente", description = "No se puede eliminar la propia cuenta ni la de un autor de formularios.")
    @DeleteMapping("/cuentas/{id}")
    public ResponseEntity<Void> eliminarCuenta(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarCuenta(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un periodo contable definitivamente", description = "Bloqueado si el mes tiene lecturas o facturas registradas.")
    @DeleteMapping("/periodos-contables/{id}")
    public ResponseEntity<Void> eliminarPeriodoContable(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarPeriodoContable(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una multa definitivamente", description = "Bloqueada si ya quedo incluida en una factura.")
    @DeleteMapping("/multas/{id}")
    public ResponseEntity<Void> eliminarMulta(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarMulta(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }
}
