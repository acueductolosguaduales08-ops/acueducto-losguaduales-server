package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.DeudaResponse;
import com.acueducto.backend.service.DeudaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "15. Deudas y Cartera", description = "Gestion de deudas, cartera y aging de facturas")
@RestController
@RequestMapping("/api/v1/deudas")
@RequiredArgsConstructor
public class DeudaController {

    private final DeudaService deudaService;

    @Operation(summary = "Obtener cartera y aging de deudas", description = "Retorna facturas vencidas/pago parcial con clasificacion por rangos de vencimiento.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/cartera")
    public ResponseEntity<DeudaResponse> obtenerCartera() {
        return ResponseEntity.ok(deudaService.obtenerCartera());
    }
}
