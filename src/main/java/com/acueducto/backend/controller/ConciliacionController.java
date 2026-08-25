package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.MovimientoBancarioResponse;
import com.acueducto.backend.service.ConciliacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Tag(name = "16. Conciliacion Bancaria", description = "Importacion de extractos y conciliacion con movimientos del sistema")
@RestController
@RequestMapping("/api/v1/conciliacion")
@RequiredArgsConstructor
public class ConciliacionController {

    private final ConciliacionService conciliacionService;

    @Operation(summary = "Importar extracto bancario (CSV)", description = "Recibe un archivo CSV con columnas: fecha, descripcion, valor, referencia (opcional).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/importar")
    public ResponseEntity<Map<String, Object>> importarExtracto(
            @RequestParam("archivo") MultipartFile archivo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> filas = new ArrayList<>();
            String linea;
            boolean esPrimera = true;
            while ((linea = reader.readLine()) != null) {
                if (esPrimera) {
                    esPrimera = false;
                    String lower = linea.toLowerCase();
                    if (lower.contains("fecha") || lower.contains("date")) continue;
                }
                String[] partes = linea.split(",");
                filas.add(partes);
            }
            String nombreArchivo = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "extracto.csv";
            return ResponseEntity.ok(conciliacionService.importarExtracto(filas, nombreArchivo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Listar movimientos bancarios pendientes")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/pendientes")
    public ResponseEntity<List<MovimientoBancarioResponse>> listarPendientes() {
        return ResponseEntity.ok(conciliacionService.listarPendientes());
    }

    @Operation(summary = "Listar todos los movimientos bancarios")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/todos")
    public ResponseEntity<List<MovimientoBancarioResponse>> listarTodos() {
        return ResponseEntity.ok(conciliacionService.listarTodos());
    }

    @Operation(summary = "Conciliar un movimiento bancario con uno del sistema")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{bancarioId}/conciliar/{tesoreriaId}")
    public ResponseEntity<MovimientoBancarioResponse> conciliar(
            @PathVariable Long bancarioId, @PathVariable Long tesoreriaId) {
        return ResponseEntity.ok(conciliacionService.conciliar(bancarioId, tesoreriaId));
    }

    @Operation(summary = "Marcar movimiento bancario sin coincidencia")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/{bancarioId}/sin-coincidencia")
    public ResponseEntity<MovimientoBancarioResponse> marcarSinCoincidencia(@PathVariable Long bancarioId) {
        return ResponseEntity.ok(conciliacionService.marcarSinCoincidencia(bancarioId));
    }

    @Operation(summary = "Resumen de conciliacion")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumen() {
        return ResponseEntity.ok(conciliacionService.obtenerResumen());
    }
}
