package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.AsociadoRequest;
import com.acueducto.backend.dto.request.CambioEstadoServicioRequest;
import com.acueducto.backend.dto.response.AsociadoResponse;
import com.acueducto.backend.dto.response.AsociadoResumenFinancieroResponse;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.service.AsociadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "02. Asociados", description = "Nucleo del sistema: registro, edicion, estado de servicio e historial de asociados (Modulo 5)")
@RestController
@RequestMapping("/api/v1/asociados")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AsociadoController {

    private final AsociadoService asociadoService;

    @Operation(summary = "Crear asociado")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<AsociadoResponse> crear(@Valid @RequestBody AsociadoRequest request) {
        return ResponseEntity.ok(asociadoService.crear(request));
    }

    @Operation(summary = "Editar asociado")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PutMapping("/{id}")
    public ResponseEntity<AsociadoResponse> editar(@PathVariable Long id, @Valid @RequestBody AsociadoRequest request) {
        return ResponseEntity.ok(asociadoService.editar(id, request));
    }

    @Operation(summary = "Buscar/listar asociados", description = "Busca por documento, nombre, apellidos o telefono (5.10).")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping
    public ResponseEntity<List<AsociadoResponse>> buscar(@Parameter(description = "Texto de busqueda") @RequestParam(required = false) String q) {
        return ResponseEntity.ok(asociadoService.buscar(q));
    }

    @Operation(summary = "Filtrar asociados por estado de servicio")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/filtrar")
    public ResponseEntity<List<AsociadoResponse>> filtrarPorEstado(@RequestParam EstadoServicio estado) {
        return ResponseEntity.ok(asociadoService.listarPorEstado(estado));
    }

    @Operation(summary = "Ver detalle de un asociado")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#id))")
    @GetMapping("/{id}")
    public ResponseEntity<AsociadoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(asociadoService.obtener(id));
    }

    @Operation(summary = "Resumen financiero del asociado", description = "Calculado dinamicamente: facturas, pagos y multas (5.4).")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or (hasRole('ASOCIADO') and @asociadoSecurity.esPropio(#id))")
    @GetMapping("/{id}/resumen-financiero")
    public ResponseEntity<AsociadoResumenFinancieroResponse> resumenFinanciero(@PathVariable Long id) {
        return ResponseEntity.ok(asociadoService.resumenFinanciero(id));
    }

    @Operation(summary = "Cambiar estado del servicio", description = "Activo o Suspendido (5.7). No afecta el acceso a la plataforma.")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PatchMapping("/{id}/estado-servicio")
    public ResponseEntity<AsociadoResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoServicioRequest request) {
        return ResponseEntity.ok(asociadoService.cambiarEstadoServicio(id, request));
    }

    @Operation(summary = "Archivar asociado (baja logica)", description = "Nunca se elimina fisicamente si tiene historial (5.8).")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archivar(@PathVariable Long id) {
        asociadoService.archivar(id);
        return ResponseEntity.noContent().build();
    }
}
