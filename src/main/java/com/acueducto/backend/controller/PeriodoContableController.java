package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.AnioContableRequest;
import com.acueducto.backend.dto.request.MesContableRequest;
import com.acueducto.backend.dto.response.AnioContableResponse;
import com.acueducto.backend.dto.response.MesContableResponse;
import com.acueducto.backend.dto.response.ResumenPeriodoResponse;
import com.acueducto.backend.service.PeriodoContableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05. Periodos Contables", description = "Anios y meses contables: apertura, cierre y reapertura de periodos (Modulo 9)")
@RestController
@RequestMapping("/api/v1/periodos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PeriodoContableController {

    private final PeriodoContableService periodoContableService;

    @Operation(summary = "Crear anio contable")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/anios")
    public ResponseEntity<AnioContableResponse> crearAnio(@Valid @RequestBody AnioContableRequest request) {
        return ResponseEntity.ok(periodoContableService.crearAnio(request));
    }

    @Operation(summary = "Listar anios contables")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/anios")
    public ResponseEntity<List<AnioContableResponse>> listarAnios() {
        return ResponseEntity.ok(periodoContableService.listarAnios());
    }

    @Operation(summary = "Crear mes contable")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/meses")
    public ResponseEntity<MesContableResponse> crearMes(@Valid @RequestBody MesContableRequest request) {
        return ResponseEntity.ok(periodoContableService.crearMes(request));
    }

    @Operation(summary = "Listar meses de un anio")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/anios/{anioId}/meses")
    public ResponseEntity<List<MesContableResponse>> listarMeses(@PathVariable Long anioId) {
        return ResponseEntity.ok(periodoContableService.listarMesesPorAnio(anioId));
    }

    @Operation(summary = "Cerrar periodo", description = "Verifica que todas las lecturas y facturas esten completas antes de cerrar (9.8).")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/meses/{mesId}/cerrar")
    public ResponseEntity<MesContableResponse> cerrarMes(@PathVariable Long mesId) {
        return ResponseEntity.ok(periodoContableService.cerrarMes(mesId));
    }

    @Operation(summary = "Reabrir periodo", description = "Uso excepcional para correcciones administrativas; queda registrado en auditoria (9.6).")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/meses/{mesId}/reabrir")
    public ResponseEntity<MesContableResponse> reabrirMes(@PathVariable Long mesId, @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(periodoContableService.reabrirMes(mesId, motivo));
    }

    @Operation(summary = "Resumen del periodo", description = "Indicadores de facturacion, tesoreria, consumo y asociados (9.9).")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/meses/{mesId}/resumen")
    public ResponseEntity<ResumenPeriodoResponse> resumenMes(@PathVariable Long mesId) {
        return ResponseEntity.ok(periodoContableService.resumenMes(mesId));
    }
}
