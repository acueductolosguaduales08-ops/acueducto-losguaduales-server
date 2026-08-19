package com.acueducto.backend.controller;

import com.acueducto.backend.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "14. Estadisticas / Dashboard", description = "Indicadores administrativos y financieros en tiempo real (1.3)")
@RestController
@RequestMapping("/api/v1/estadisticas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @Operation(summary = "Dashboard general del sistema")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(estadisticasService.dashboardGeneral());
    }
}
