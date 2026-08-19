package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.MedidorRequest;
import com.acueducto.backend.dto.response.MedidorResponse;
import com.acueducto.backend.entity.enums.EstadoMedidor;
import com.acueducto.backend.service.MedidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03. Medidores", description = "Registro y administracion de medidores de agua (Modulo 6). Exclusivo del Administrador.")
@RestController
@RequestMapping("/api/v1/medidores")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MedidorController {

    private final MedidorService medidorService;

    @Operation(summary = "Registrar medidor")
    @PostMapping
    public ResponseEntity<MedidorResponse> crear(@Valid @RequestBody MedidorRequest request) {
        return ResponseEntity.ok(medidorService.crear(request));
    }

    @Operation(summary = "Editar medidor")
    @PutMapping("/{id}")
    public ResponseEntity<MedidorResponse> editar(@PathVariable Long id, @Valid @RequestBody MedidorRequest request) {
        return ResponseEntity.ok(medidorService.editar(id, request));
    }

    @Operation(summary = "Cambiar estado del medidor", description = "Activo, En mantenimiento, Danado o Retirado (6.4).")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<MedidorResponse> cambiarEstado(@PathVariable Long id, @RequestParam EstadoMedidor estado) {
        return ResponseEntity.ok(medidorService.cambiarEstado(id, estado));
    }

    @Operation(summary = "Listar medidores")
    @GetMapping
    public ResponseEntity<List<MedidorResponse>> listar() {
        return ResponseEntity.ok(medidorService.listar());
    }

    @Operation(summary = "Ver detalle de un medidor")
    @GetMapping("/{id}")
    public ResponseEntity<MedidorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(medidorService.obtener(id));
    }
}
