package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.ConfirmarPasswordRequest;
import com.acueducto.backend.dto.response.VerificacionBorradoResponse;
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
 * Modulo "Gestion de datos importantes" (5): eliminacion DEFINITIVA e irreversible de todos los
 * tipos de datos del sistema, en CASCADA (si un registro tiene historial, ese historial tambien
 * se borra). Exclusivo del Administrador. Cada borrado pide reconfirmar la contrasena en el body,
 * aunque ya haya sesion iniciada, y queda registrado en auditoria.
 *
 * Antes de borrar, el frontend debe consultar GET /verificar (informa exactamente que se va a
 * borrar y en cascada) y mostrar una confirmacion explicita, p. ej. "esto se va a perder para
 * siempre, ¿continuar?".
 */
@Tag(name = "12. Gestion de datos importantes", description = "Eliminacion definitiva e irreversible en cascada. Solo Administrador, pide reconfirmar contrasena.")
@RestController
@RequestMapping("/api/v1/datos-importantes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class GestionDatosImportantesController {

    private final GestionDatosImportantesService service;

    @Operation(summary = "Verificar antes de borrar", description = "Informa si el registro se puede borrar y que se eliminara en cascada. Debe consultarse antes de pedir la contrasena.")
    @GetMapping("/verificar")
    public ResponseEntity<VerificacionBorradoResponse> verificar(
            @RequestParam String tipo,
            @RequestParam Long id,
            Authentication auth) {
        return ResponseEntity.ok(service.verificar(tipo, id, auth.getName()));
    }

    @Operation(summary = "Eliminar un formulario definitivamente", description = "Borra en cascada sus preguntas y las respuestas ya recibidas.")
    @DeleteMapping("/formularios/{id}")
    public ResponseEntity<Void> eliminarFormulario(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarFormulario(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una factura definitivamente", description = "Borra en cascada sus pagos, recibos, movimientos y conceptos; libera su lectura.")
    @DeleteMapping("/facturas/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarFactura(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un recibo definitivamente", description = "Borra el recibo y sus movimientos; el pago y la factura no se tocan.")
    @DeleteMapping("/recibos/{id}")
    public ResponseEntity<Void> eliminarRecibo(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarRecibo(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un asociado definitivamente", description = "Borra en cascada todo su historial (facturas, pagos, recibos, multas, lecturas, movimientos y cuenta). El medidor solo se desvincula, no se borra.")
    @DeleteMapping("/asociados/{id}")
    public ResponseEntity<Void> eliminarAsociado(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarAsociado(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una cuenta definitivamente", description = "Borra en cascada formularios, notificaciones, pagos y movimientos de esa cuenta. No se puede borrar la propia cuenta en uso.")
    @DeleteMapping("/cuentas/{id}")
    public ResponseEntity<Void> eliminarCuenta(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarCuenta(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un periodo contable (mes) definitivamente", description = "Borra en cascada sus facturas, lecturas y movimientos.")
    @DeleteMapping("/periodos-contables/{id}")
    public ResponseEntity<Void> eliminarPeriodoContable(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarPeriodoContable(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un anio contable definitivamente", description = "Borra en cascada todos sus meses con todo lo que contengan.")
    @DeleteMapping("/anios-contables/{id}")
    public ResponseEntity<Void> eliminarAnioContable(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarAnioContable(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una multa definitivamente", description = "Si estaba incluida en una factura, descuenta su valor del total de esa factura.")
    @DeleteMapping("/multas/{id}")
    public ResponseEntity<Void> eliminarMulta(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarMulta(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un medidor definitivamente", description = "Desvincula al asociado (no lo borra) y borra sus lecturas en cascada.")
    @DeleteMapping("/medidores/{id}")
    public ResponseEntity<Void> eliminarMedidor(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarMedidor(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una lectura definitivamente", description = "Si genero factura, la factura se borra en cascada.")
    @DeleteMapping("/lecturas/{id}")
    public ResponseEntity<Void> eliminarLectura(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarLectura(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un pago definitivamente", description = "Borra en cascada su recibo y movimientos, y recalcula el estado de la factura.")
    @DeleteMapping("/pagos/{id}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarPago(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar un movimiento de tesoreria definitivamente", description = "Si es entrada de un pago, el pago se borra en cascada; si es ingreso/gasto suelto, solo el movimiento.")
    @DeleteMapping("/movimientos/{id}")
    public ResponseEntity<Void> eliminarMovimientoTesoreria(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarMovimientoTesoreria(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una notificacion definitivamente", description = "Borra la notificacion junto con sus registros de lectura.")
    @DeleteMapping("/notificaciones/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id, Authentication auth, @Valid @RequestBody ConfirmarPasswordRequest request) {
        service.eliminarNotificacion(id, auth.getName(), request.password());
        return ResponseEntity.noContent().build();
    }
}