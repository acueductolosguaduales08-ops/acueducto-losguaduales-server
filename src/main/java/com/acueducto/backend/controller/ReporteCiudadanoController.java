package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.ReporteCiudadanoRequest;
import com.acueducto.backend.dto.response.ReporteCiudadanoResponse;
import com.acueducto.backend.service.ReporteCiudadanoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modulo de Reportes Ciudadanos (fugas y quejas/reclamos). Es una funcion aparte, independiente
 * de los demas modulos: el envio es publico y sin inicio de sesion, cada reporte es temporal
 * y se elimina automaticamente 8 dias despues de haber sido creado.
 */
@Tag(name = "17. Reportes Ciudadanos", description = "Reporte publico de fugas y quejas/reclamos, sin inicio de sesion. Los reportes son temporales (se eliminan a los 8 dias).")
@RestController
@RequiredArgsConstructor
public class ReporteCiudadanoController {

    private final ReporteCiudadanoService reporteCiudadanoService;

    @Operation(summary = "Reportar una fuga o enviar una queja/reclamo",
            description = "Publico, sin necesidad de iniciar sesion. Nombre y mensaje son obligatorios; el contacto (telefono o correo) es opcional. "
                    + "El reporte se elimina automaticamente 8 dias despues de creado.")
    @PostMapping("/api/v1/publico/reportes")
    public ResponseEntity<ReporteCiudadanoResponse> crear(@Valid @RequestBody ReporteCiudadanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteCiudadanoService.crear(request));
    }

    @Operation(summary = "Listar todos los reportes ciudadanos",
            description = "Exclusivo del Administrador y el Tesorero. Muestra nombre, mensaje, contacto, "
                    + "fecha de creacion y fecha en la que se eliminara cada reporte.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/api/v1/reportes")
    public ResponseEntity<Page<ReporteCiudadanoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(reporteCiudadanoService.listarTodos(pageable));
    }
}
