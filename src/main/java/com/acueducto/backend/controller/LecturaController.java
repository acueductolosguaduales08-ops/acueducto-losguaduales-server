package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.LecturaRequest;
import com.acueducto.backend.dto.response.LecturaResponse;
import com.acueducto.backend.service.LecturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04. Lecturas y Consumo", description = "Registro de lecturas de medidor y calculo automatico del consumo (Modulo 6). Exclusivo del Administrador.")
@RestController
@RequestMapping("/api/v1/lecturas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class LecturaController {

    private final LecturaService lecturaService;

    @Operation(summary = "Registrar lectura", description = "Calcula el consumo automaticamente (actual - anterior). Nunca puede ser negativo (6.6).")
    @PostMapping
    public ResponseEntity<LecturaResponse> registrar(@Valid @RequestBody LecturaRequest request) {
        return ResponseEntity.ok(lecturaService.registrar(request));
    }

    @Operation(summary = "Editar lectura", description = "Solo si aun no ha generado factura (6.14).")
    @PutMapping("/{id}")
    public ResponseEntity<LecturaResponse> editar(@PathVariable Long id, @Valid @RequestBody LecturaRequest request) {
        return ResponseEntity.ok(lecturaService.editar(id, request));
    }

    @Operation(summary = "Ver detalle de una lectura")
    @GetMapping("/{id}")
    public ResponseEntity<LecturaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(lecturaService.obtener(id));
    }

    @Operation(summary = "Historial de consumo de un asociado", description = "Todas las lecturas registradas (6.10).")
    @GetMapping("/asociado/{asociadoId}")
    public ResponseEntity<List<LecturaResponse>> historialPorAsociado(@PathVariable Long asociadoId) {
        return ResponseEntity.ok(lecturaService.historialPorAsociado(asociadoId));
    }

    @Operation(summary = "Lecturas registradas en un periodo", description = "Usado antes de ejecutar la Generacion de Facturacion del Mes (6.14).")
    @GetMapping("/mes/{mesContableId}")
    public ResponseEntity<List<LecturaResponse>> porMes(@PathVariable Long mesContableId) {
        return ResponseEntity.ok(lecturaService.porMes(mesContableId));
    }
}
