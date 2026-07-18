package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.CategoriaRequest;
import com.acueducto.backend.dto.request.EtiquetaRequest;
import com.acueducto.backend.dto.request.PublicacionRequest;
import com.acueducto.backend.dto.request.VideoRequest;
import com.acueducto.backend.dto.response.PublicacionResponse;
import com.acueducto.backend.dto.response.ReaccionResponse;
import com.acueducto.backend.entity.Categoria;
import com.acueducto.backend.entity.Etiqueta;
import com.acueducto.backend.entity.Video;
import com.acueducto.backend.service.PublicacionService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "12. Portal Publico", description = "Galeria, publicaciones destacadas, categorias, etiquetas y videos (Modulo 11)")
@RestController
@RequestMapping("/api/v1/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;

    @Operation(summary = "Crear publicacion")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<PublicacionResponse> crear(@Valid @RequestBody PublicacionRequest request, Authentication authentication) {
        return ResponseEntity.ok(publicacionService.crear(request, authentication.getName()));
    }

    @Operation(summary = "Publicar contenido")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/publicar")
    public ResponseEntity<PublicacionResponse> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.publicar(id));
    }

    @Operation(summary = "Ocultar contenido")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/ocultar")
    public ResponseEntity<PublicacionResponse> ocultar(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.ocultar(id));
    }

    @Operation(summary = "Destacar/quitar destacado")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PatchMapping("/{id}/destacar")
    public ResponseEntity<PublicacionResponse> destacar(@PathVariable Long id, @RequestParam boolean destacada) {
        return ResponseEntity.ok(publicacionService.destacar(id, destacada));
    }

    @Operation(summary = "Editar publicacion")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PublicacionRequest request) {
        return ResponseEntity.ok(publicacionService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar publicacion definitivamente", description = "Exclusivo del Administrador (11.10).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        publicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listado completo para el panel de edicion", description = "Incluye borradores y ocultas, no solo publicadas. Tesorero/Administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/admin")
    public ResponseEntity<Page<PublicacionResponse>> listarTodas(Pageable pageable) {
        return ResponseEntity.ok(publicacionService.listarTodas(pageable));
    }

    @Operation(summary = "Ver el detalle de una publicacion")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.obtener(id));
    }

    @Operation(summary = "Galeria publica", description = "Publicaciones publicadas, disponible sin login (11.5).")
    @GetMapping("/publicas")
    public ResponseEntity<Page<PublicacionResponse>> listarPublicas(Pageable pageable) {
        return ResponseEntity.ok(publicacionService.listarPublicadas(pageable));
    }

    @Operation(summary = "Publicaciones destacadas", description = "Disponible sin login (11.6).")
    @GetMapping("/destacadas")
    public ResponseEntity<List<PublicacionResponse>> listarDestacadas() {
        return ResponseEntity.ok(publicacionService.listarDestacadas());
    }

    // ---- Categorias ----
    @Operation(summary = "Crear categoria")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/categorias")
    public ResponseEntity<Categoria> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(publicacionService.crearCategoria(request));
    }

    @Operation(summary = "Listar categorias")
    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(publicacionService.listarCategorias());
    }

    // ---- Etiquetas ----
    @Operation(summary = "Crear etiqueta")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/etiquetas")
    public ResponseEntity<Etiqueta> crearEtiqueta(@Valid @RequestBody EtiquetaRequest request) {
        return ResponseEntity.ok(publicacionService.crearEtiqueta(request));
    }

    @Operation(summary = "Listar etiquetas")
    @GetMapping("/etiquetas")
    public ResponseEntity<List<Etiqueta>> listarEtiquetas() {
        return ResponseEntity.ok(publicacionService.listarEtiquetas());
    }

    // ---- Videos ----
    @Operation(summary = "Publicar video")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/videos")
    public ResponseEntity<Video> crearVideo(@Valid @RequestBody VideoRequest request) {
        return ResponseEntity.ok(publicacionService.crearVideo(request));
    }

    @Operation(summary = "Listar videos publicos")
    @GetMapping("/videos/publicos")
    public ResponseEntity<List<Video>> listarVideos() {
        return ResponseEntity.ok(publicacionService.listarVideosVisibles());
    }

    @Operation(summary = "Ocultar video")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PatchMapping("/videos/{id}/ocultar")
    public ResponseEntity<Void> ocultarVideo(@PathVariable Long id) {
        publicacionService.ocultarVideo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Reacciones (publico, sin login) ----

    @Operation(summary = "Ver reacciones de una publicacion", description = "Conteo total por cada emoji. Publico (11.x).")
    @GetMapping("/{id}/reacciones")
    public ResponseEntity<List<ReaccionResponse>> listarReacciones(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.listarReacciones(id));
    }

    @Operation(summary = "Reaccionar a una publicacion", description = "Suma 1 al contador del emoji indicado. Publico, no requiere sesion. "
            + "Evitar reacciones repetidas del mismo dispositivo es responsabilidad del frontend.")
    @PostMapping("/{id}/reacciones")
    public ResponseEntity<ReaccionResponse> reaccionar(@PathVariable Long id, @RequestParam String emoji) {
        return ResponseEntity.ok(publicacionService.reaccionar(id, emoji));
    }

    @Operation(summary = "Quitar una reaccion propia", description = "Resta 1 al contador del emoji indicado (nunca baja de cero). Publico.")
    @DeleteMapping("/{id}/reacciones")
    public ResponseEntity<Void> quitarReaccion(@PathVariable Long id, @RequestParam String emoji) {
        publicacionService.quitarReaccion(id, emoji);
        return ResponseEntity.noContent().build();
    }
}
