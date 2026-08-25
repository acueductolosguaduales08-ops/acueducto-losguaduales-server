package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.NotaCreditoRequest;
import com.acueducto.backend.dto.response.NotaCreditoResponse;
import com.acueducto.backend.service.NotaCreditoService;
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

import java.util.Map;

@Tag(name = "14. Notas de Credito", description = "Ajustes y notas de credito")
@RestController
@RequestMapping("/api/v1/notas-credito")
@RequiredArgsConstructor
public class NotaCreditoController {

    private final NotaCreditoService notaCreditoService;

    @Operation(summary = "Crear una nota de credito")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<NotaCreditoResponse> crear(@Valid @RequestBody NotaCreditoRequest request) {
        return ResponseEntity.ok(notaCreditoService.crear(request));
    }

    @Operation(summary = "Listar notas de credito paginadas")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping
    public ResponseEntity<Page<NotaCreditoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(notaCreditoService.listar(pageable));
    }

    @Operation(summary = "Obtener nota de credito por ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<NotaCreditoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notaCreditoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar notas de credito por asociado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/asociado/{asociadoId}")
    public ResponseEntity<Page<NotaCreditoResponse>> listarPorAsociado(
            @PathVariable Long asociadoId, Pageable pageable) {
        return ResponseEntity.ok(notaCreditoService.listarPorAsociado(asociadoId, pageable));
    }

    @Operation(summary = "Aplicar nota de credito a una factura")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/aplicar")
    public ResponseEntity<NotaCreditoResponse> aplicar(@PathVariable Long id) {
        return ResponseEntity.ok(notaCreditoService.aplicar(id));
    }

    @Operation(summary = "Anular una nota de credito")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/anular")
    public ResponseEntity<NotaCreditoResponse> anular(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(notaCreditoService.anular(id, body.getOrDefault("motivo", "Sin motivo")));
    }
}
