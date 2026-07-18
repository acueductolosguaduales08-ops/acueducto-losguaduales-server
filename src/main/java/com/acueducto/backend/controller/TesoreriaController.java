package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.MovimientoTesoreriaRequest;
import com.acueducto.backend.dto.request.MultaRequest;
import com.acueducto.backend.dto.request.RegistrarPagoRequest;
import com.acueducto.backend.dto.response.*;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.service.TesoreriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "08. Tesoreria", description = "Pagos, multas, ingresos, gastos y caja diaria (Modulo 8)")
@RestController
@RequestMapping("/api/v1/tesoreria")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
public class TesoreriaController {

    private final TesoreriaService tesoreriaService;

    @Operation(summary = "Registrar pago de factura", description = "Operacion atomica: actualiza la factura, crea el movimiento, genera el recibo y notifica al asociado (8.5).")
    @PostMapping("/pagos")
    public ResponseEntity<PagoResponse> registrarPago(@Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.ok(tesoreriaService.registrarPago(request));
    }

    @Operation(summary = "Registrar multa")
    @PostMapping("/multas")
    public ResponseEntity<MultaResponse> registrarMulta(@Valid @RequestBody MultaRequest request) {
        return ResponseEntity.ok(tesoreriaService.registrarMulta(request));
    }

    @Operation(summary = "Listar multas de un asociado")
    @GetMapping("/multas/asociado/{asociadoId}")
    public ResponseEntity<List<MultaResponse>> listarMultas(@PathVariable Long asociadoId) {
        return ResponseEntity.ok(tesoreriaService.listarMultasPorAsociado(asociadoId));
    }

    @Operation(summary = "Registrar ingreso extraordinario", description = "Donaciones, reconexiones, nuevas afiliaciones, otros ingresos (8.4).")
    @PostMapping("/ingresos")
    public ResponseEntity<MovimientoTesoreriaResponse> registrarIngreso(@Valid @RequestBody MovimientoTesoreriaRequest request) {
        return ResponseEntity.ok(tesoreriaService.registrarIngreso(request));
    }

    @Operation(summary = "Registrar gasto", description = "Servicios, materiales, reparaciones, personal, otros egresos (8.9).")
    @PostMapping("/gastos")
    public ResponseEntity<MovimientoTesoreriaResponse> registrarGasto(@Valid @RequestBody MovimientoTesoreriaRequest request) {
        return ResponseEntity.ok(tesoreriaService.registrarGasto(request));
    }

    @Operation(summary = "Anular movimiento", description = "Exclusivo del Administrador (8.3).")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/movimientos/{id}/anular")
    public ResponseEntity<Void> anularMovimiento(@PathVariable Long id, @RequestParam String motivo) {
        tesoreriaService.anularMovimiento(id, motivo);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar movimientos por tipo (entradas o salidas)")
    @GetMapping("/movimientos")
    public ResponseEntity<Page<MovimientoTesoreriaResponse>> listarMovimientos(@RequestParam TipoMovimiento tipo, Pageable pageable) {
        return ResponseEntity.ok(tesoreriaService.listarMovimientos(tipo, pageable));
    }

    @Operation(summary = "Historial combinado de movimientos", description = "Entradas y salidas juntas en una sola lista, ordenadas (ej: ?sort=fecha,desc).")
    @GetMapping("/movimientos/todos")
    public ResponseEntity<Page<MovimientoTesoreriaResponse>> listarTodosLosMovimientos(Pageable pageable) {
        return ResponseEntity.ok(tesoreriaService.listarTodosLosMovimientos(pageable));
    }

    @Operation(summary = "Caja diaria", description = "Ingresos, gastos y balance del dia actual (8.10).")
    @GetMapping("/caja-diaria")
    public ResponseEntity<CajaDiariaResponse> cajaDiaria() {
        return ResponseEntity.ok(tesoreriaService.cajaDiaria());
    }
}
