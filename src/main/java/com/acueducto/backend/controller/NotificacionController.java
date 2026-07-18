package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.NotificacionRequest;
import com.acueducto.backend.dto.response.NotificacionResponse;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "11. Notificaciones", description = "Avisos publicos, para asociados y administrativos (Modulo 13)")
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Crear notificacion")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<NotificacionResponse> crear(@Valid @RequestBody NotificacionRequest request, Authentication authentication) {
        return ResponseEntity.ok(notificacionService.crear(request, authentication.getName()));
    }

    @Operation(summary = "Listar notificaciones publicas", description = "Disponible sin iniciar sesion (2.7).")
    @GetMapping("/publicas")
    public ResponseEntity<Page<NotificacionResponse>> listarPublicas(Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listarPublicas(pageable));
    }

    @Operation(summary = "Listar mis notificaciones", description = "Notificaciones personales del usuario autenticado (13.11).")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mis-notificaciones")
    public ResponseEntity<Page<NotificacionResponse>> misNotificaciones(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(principal.getId(), pageable));
    }

    @Operation(summary = "Marcar notificacion como leida")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificacionService.marcarLeida(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar notificacion definitivamente", description = "Exclusivo del Administrador (13.12).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
