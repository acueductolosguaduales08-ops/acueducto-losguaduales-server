package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.FacturaResponse;
import com.acueducto.backend.dto.response.ReciboResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.security.UserPrincipal;
import com.acueducto.backend.service.FacturaService;
import com.acueducto.backend.service.TesoreriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Modulo de Consultas Publicas (1.4 / 2.7): permite a cualquier persona consultar el estado
 * del servicio de un predio sin iniciar sesion, ingresando el documento del asociado.
 * No expone informacion financiera detallada, solo el estado general del servicio.
 */
@Tag(name = "15. Consultas Publicas", description = "Consulta de estado de servicio sin necesidad de iniciar sesion (1.4 / 2.7)")
@RestController
@RequestMapping("/api/v1/consultas")
@RequiredArgsConstructor
public class PublicoController {

    private final AsociadoRepository asociadoRepository;
    private final FacturaService facturaService;
    private final TesoreriaService tesoreriaService;

    @Operation(summary = "Consultar estado del servicio por documento",
            description = "Consulta publica minima: confirma si el documento esta registrado y el estado del servicio. No expone nombres ni codigos internos.")
    @GetMapping("/estado-servicio")
    public ResponseEntity<EstadoServicioPublicoResponse> consultarEstadoServicio(@RequestParam String documento) {
        Asociado asociado = asociadoRepository.findByDocumento(documento)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro un asociado con ese documento."));

        return ResponseEntity.ok(new EstadoServicioPublicoResponse(
                asociado.getEstadoServicio()
        ));
    }

    @Operation(summary = "Consultar estado de una factura por numero (publico)",
            description = "Consulta sin login: muestra solo estado, saldo pendiente y confirmacion de existencia. No expone datos del asociado.")
    @GetMapping("/factura/{numeroFactura}")
    public ResponseEntity<FacturaPublicoResponse> consultarFacturaPublica(@PathVariable String numeroFactura) {
        Factura factura = facturaService.obtenerPorNumero(numeroFactura);
        return ResponseEntity.ok(new FacturaPublicoResponse(
                factura.getEstado(),
                factura.getSaldoPendiente(),
                factura.getFechaLimitePago()
        ));
    }

    @Operation(summary = "Consultar factura con detalles completos (autenticado)",
            description = "Requiere login. Asociado solo ve facturas propias. Admin/Tesorero ve todas.")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO') or hasRole('ASOCIADO')")
    @GetMapping("/factura/{numeroFactura}/detalle")
    public ResponseEntity<FacturaResponse> consultarFacturaDetalle(
            @PathVariable String numeroFactura, @AuthenticationPrincipal UserPrincipal principal) {
        Factura factura = facturaService.obtenerPorNumero(numeroFactura);
        if (principal.getUsuario().getRol() == com.acueducto.backend.entity.enums.Rol.ASOCIADO) {
            Long asociadoIdUsuario = principal.getUsuario().getAsociado() != null
                    ? principal.getUsuario().getAsociado().getId() : null;
            if (asociadoIdUsuario == null || !asociadoIdUsuario.equals(factura.getAsociado().getId())) {
                throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                        "Solo puede consultar facturas propias.");
            }
        }
        return ResponseEntity.ok(FacturaResponse.fromEntity(factura));
    }

    @Getter
    @Setter
    public static class EstadoServicioPublicoResponse {
        private EstadoServicio estadoServicio;

        public EstadoServicioPublicoResponse(EstadoServicio estadoServicio) {
            this.estadoServicio = estadoServicio;
        }
    }

    @Getter
    @Setter
    public static class FacturaPublicoResponse {
        private com.acueducto.backend.entity.enums.EstadoFactura estado;
        private java.math.BigDecimal saldoPendiente;
        private java.time.LocalDate fechaLimitePago;

        public FacturaPublicoResponse(com.acueducto.backend.entity.enums.EstadoFactura estado,
                java.math.BigDecimal saldoPendiente, java.time.LocalDate fechaLimitePago) {
            this.estado = estado;
            this.saldoPendiente = saldoPendiente;
            this.fechaLimitePago = fechaLimitePago;
        }
    }
}
