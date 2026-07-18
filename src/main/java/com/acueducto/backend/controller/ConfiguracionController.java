package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.ArchivoInstitucionalUrlRequest;
import com.acueducto.backend.dto.request.ConfiguracionRequest;
import com.acueducto.backend.dto.request.MetodoPagoRequest;
import com.acueducto.backend.dto.response.ConfiguracionResponse;
import com.acueducto.backend.entity.ArchivoInstitucional;
import com.acueducto.backend.entity.MetodoPago;
import com.acueducto.backend.entity.enums.TipoArchivoInstitucional;
import jakarta.validation.Valid;
import com.acueducto.backend.service.ConfiguracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "06. Configuracion del Sistema", description = "Parametros institucionales, tarifas, metodos de pago y archivos institucionales (Modulo 10). Exclusivo del Administrador para modificar.")
@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @Operation(summary = "Consultar configuracion actual")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping
    public ResponseEntity<ConfiguracionResponse> obtener() {
        return ResponseEntity.ok(configuracionService.obtener());
    }

    @Operation(summary = "Actualizar configuracion", description = "Los cambios de tarifa solo afectan las facturas nuevas (6.7 / 10.6).")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping
    public ResponseEntity<ConfiguracionResponse> actualizar(@Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionService.actualizar(request));
    }

    @Operation(summary = "Crear metodo de pago")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/metodos-pago")
    public ResponseEntity<MetodoPago> crearMetodoPago(@Valid @RequestBody MetodoPagoRequest request) {
        return ResponseEntity.ok(configuracionService.crearMetodoPago(request));
    }

    @Operation(summary = "Activar/desactivar metodo de pago")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/metodos-pago/{id}")
    public ResponseEntity<MetodoPago> cambiarEstadoMetodoPago(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(configuracionService.cambiarEstadoMetodoPago(id, activo));
    }

    @Operation(summary = "Listar metodos de pago disponibles (activos)")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @GetMapping("/metodos-pago")
    public ResponseEntity<List<MetodoPago>> listarMetodosPago() {
        return ResponseEntity.ok(configuracionService.listarMetodosPagoActivos());
    }

    @Operation(summary = "Registrar archivo institucional desde URL", description = "Logo, firma o sello institucional (10.9). Los archivos institucionales solo se manejan por URL (igual que las imagenes de publicaciones): no se sube ni se guarda ningun archivo en el servidor, solo se registra el enlace. Acepta JSON con la URL en el body para evitar problemas con caracteres especiales o URLs largas.")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/archivos/{tipo}/url")
    public ResponseEntity<ArchivoInstitucional> subirArchivoDesdeUrl(@PathVariable TipoArchivoInstitucional tipo,
                                                                     @Valid @RequestBody ArchivoInstitucionalUrlRequest request) {
        return ResponseEntity.ok(configuracionService.subirArchivoInstitucionalDesdeUrl(tipo, request.url(), request.nombreArchivo()));
    }

    @Operation(summary = "Activar archivo institucional", description = "Selecciona cual logo/firma/sello se usara en los documentos (10.10).")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/archivos/{archivoId}/activar")
    public ResponseEntity<Void> activarArchivo(@PathVariable Long archivoId) {
        configuracionService.activarArchivoInstitucional(archivoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar archivo institucional registrado", description = "Quita una URL de logo/firma/sello de la lista de guardadas. Si estaba activa, deja de usarse en los documentos nuevos.")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/archivos/{archivoId}")
    public ResponseEntity<Void> eliminarArchivo(@PathVariable Long archivoId) {
        configuracionService.eliminarArchivoInstitucional(archivoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar archivos institucionales por tipo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/archivos/{tipo}")
    public ResponseEntity<List<ArchivoInstitucional>> listarArchivos(@PathVariable TipoArchivoInstitucional tipo) {
        return ResponseEntity.ok(configuracionService.listarArchivosPorTipo(tipo));
    }
}
