package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.EncuestaRequest;
import com.acueducto.backend.dto.request.ResponderEncuestaRequest;
import com.acueducto.backend.dto.response.EncuestaEstadisticasResponse;
import com.acueducto.backend.dto.response.EncuestaResponse;
import com.acueducto.backend.dto.response.RespuestaEncuestaResponse;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.EncuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "10. Encuestas y Formularios", description = "Formularios dinamicos para la comunidad (Modulo 12)")
@RestController
@RequestMapping("/api/v1/encuestas")
@RequiredArgsConstructor
public class EncuestaController {

    private final EncuestaService encuestaService;

    @Operation(summary = "Crear formulario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping
    public ResponseEntity<EncuestaResponse> crear(@Valid @RequestBody EncuestaRequest request,
                                                   org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(encuestaService.crear(request, authentication.getName()));
    }

    @Operation(summary = "Activar formulario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/activar")
    public ResponseEntity<EncuestaResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(encuestaService.activar(id));
    }

    @Operation(summary = "Desactivar formulario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<EncuestaResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(encuestaService.desactivar(id));
    }

    @Operation(summary = "Archivar formulario", description = "No se eliminan formularios con respuestas registradas (12.13).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archivar(@PathVariable Long id) {
        encuestaService.archivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar todos los formularios (administracion)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/admin")
    public ResponseEntity<List<EncuestaResponse>> listarTodas() {
        return ResponseEntity.ok(encuestaService.listarTodas());
    }

    @Operation(summary = "Ver estadisticas de participacion de un formulario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<EncuestaEstadisticasResponse> estadisticas(@PathVariable Long id) {
        return ResponseEntity.ok(encuestaService.estadisticas(id));
    }

    @Operation(summary = "Ver todas las respuestas de un formulario",
            description = "Muestra cada respuesta con el nombre de quien respondio (o \"Anonimo\" si el formulario es anonimo o no se identifico).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/{id}/respuestas")
    public ResponseEntity<List<RespuestaEncuestaResponse>> listarRespuestas(@PathVariable Long id) {
        return ResponseEntity.ok(encuestaService.listarRespuestas(id));
    }

    @Operation(summary = "Listar formularios activos (publico)", description = "Disponible sin iniciar sesion (2.7).")
    @GetMapping("/publicas")
    public ResponseEntity<List<EncuestaResponse>> listarActivas() {
        return ResponseEntity.ok(encuestaService.listarActivas());
    }

    @Operation(summary = "Ver un formulario por codigo", description = "Usado al escanear el codigo QR del formulario (12.14). Publico.")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<EncuestaResponse> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(encuestaService.obtenerPorCodigo(codigo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EncuestaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(encuestaService.obtener(id));
    }

    @Operation(summary = "Responder un formulario", description = "Disponible para publico o asociados segun configuracion (2.7 / 12.13).")
    @PostMapping("/{id}/responder")
    public ResponseEntity<Void> responder(@PathVariable Long id, @Valid @RequestBody ResponderEncuestaRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest httpRequest) {
        encuestaService.responder(id, request, principal != null ? principal.getUsuario() : null, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
