package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.HeroLinkRequest;
import com.acueducto.backend.dto.request.ModoHeroRequest;
import com.acueducto.backend.dto.response.HeroActualResponse;
import com.acueducto.backend.dto.response.HeroLinkResponse;
import com.acueducto.backend.service.HeroLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hero/banner del portal publico o la app. Se pueden registrar varios links; segun el modo
 * activo se muestra siempre el mismo (marcado como principal) o uno al azar que rota cada
 * 15 minutos. Consultar el hero actual es publico (sin login); administrarlo (agregar, borrar,
 * elegir principal, cambiar modo) es exclusivo de Administrador y Tesorero.
 */
@Tag(name = "11. Hero / Banner", description = "Hero/banner del portal publico o la app: varios links registrados, modo unico o aleatorio cada 15 minutos.")
@RestController
@RequestMapping("/api/v1/configuracion/hero")
@RequiredArgsConstructor
public class HeroLinkController {

    private final HeroLinkService heroLinkService;

    @Operation(summary = "Hero actual", description = "Publico, sin login. Devuelve el link que corresponde mostrar ahora mismo segun el modo activo "
            + "(el marcado como principal, o uno al azar vigente por 15 minutos). link puede venir null si todavia no hay ninguno registrado.")
    @GetMapping("/actual")
    public ResponseEntity<HeroActualResponse> heroActual() {
        return ResponseEntity.ok(heroLinkService.heroActual());
    }

    @Operation(summary = "Agregar un hero", description = "Acepta el link en el body (no en la URL ni un query param) para que un link largo o con "
            + "caracteres especiales no de problema. El primer hero que se agrega queda como principal automaticamente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<HeroLinkResponse> agregar(@Valid @RequestBody HeroLinkRequest request) {
        return ResponseEntity.ok(heroLinkService.agregar(request.link()));
    }

    @Operation(summary = "Listar todos los heros registrados")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping
    public ResponseEntity<List<HeroLinkResponse>> listarTodos() {
        return ResponseEntity.ok(heroLinkService.listarTodos());
    }

    @Operation(summary = "Eliminar un hero definitivamente", description = "Si era el principal, se promueve otro automaticamente (si queda alguno).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        heroLinkService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Elegir hero principal", description = "Solo tiene efecto visible cuando el modo activo es UNICO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PatchMapping("/{id}/principal")
    public ResponseEntity<HeroLinkResponse> elegirPrincipal(@PathVariable Long id) {
        return ResponseEntity.ok(heroLinkService.elegirPrincipal(id));
    }

    @Operation(summary = "Cambiar el modo del hero", description = "UNICO: siempre el principal. ALEATORIO_15MIN: uno al azar, cambia cada 15 minutos.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PutMapping("/modo")
    public ResponseEntity<Void> cambiarModo(@Valid @RequestBody ModoHeroRequest request) {
        heroLinkService.cambiarModo(request.modo());
        return ResponseEntity.noContent().build();
    }
}
