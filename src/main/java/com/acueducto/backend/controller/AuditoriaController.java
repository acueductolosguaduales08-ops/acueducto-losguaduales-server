package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.AuditoriaResponse;
import com.acueducto.backend.repository.AuditoriaRepository;
import com.acueducto.backend.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "13. Auditoria", description = "Historial de acciones importantes del sistema (2.13 / 4.4). Exclusivo del Administrador.")
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaService auditoriaService;

    @Operation(summary = "Listar toda la auditoria")
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findAll(pageable).map(AuditoriaResponse::fromEntity));
    }

    @Operation(summary = "Filtrar auditoria por modulo")
    @GetMapping("/modulo/{modulo}")
    public ResponseEntity<Page<AuditoriaResponse>> porModulo(@PathVariable String modulo, Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findByModulo(modulo, pageable).map(AuditoriaResponse::fromEntity));
    }

    @Operation(summary = "Filtrar auditoria por usuario")
    @GetMapping("/usuario/{usuario}")
    public ResponseEntity<Page<AuditoriaResponse>> porUsuario(@PathVariable String usuario, Pageable pageable) {
        return ResponseEntity.ok(auditoriaRepository.findByUsuario(usuario, pageable).map(AuditoriaResponse::fromEntity));
    }

    @Operation(summary = "Consultar si la auditoria esta activa")
    @GetMapping("/estado")
    public ResponseEntity<EstadoAuditoria> estado() {
        return ResponseEntity.ok(new EstadoAuditoria(auditoriaService.estaActiva()));
    }

    @Operation(summary = "Desactivar la auditoria", description = "Deja de registrar nuevas acciones. Esta misma accion siempre queda registrada como el ultimo movimiento, indicando quien la solicito.")
    @PatchMapping("/desactivar")
    public ResponseEntity<Void> desactivar(@RequestParam @NotBlank String nombre) {
        auditoriaService.desactivar(nombre);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar la auditoria")
    @PatchMapping("/activar")
    public ResponseEntity<Void> activar(@RequestParam @NotBlank String nombre) {
        auditoriaService.activar(nombre);
        return ResponseEntity.noContent().build();
    }

    public record EstadoAuditoria(boolean activa) {
    }
}

