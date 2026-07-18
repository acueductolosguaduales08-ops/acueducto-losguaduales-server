package com.acueducto.backend.controller;

import com.acueducto.backend.dto.response.FacturaResponse;
import com.acueducto.backend.dto.response.ReciboResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.service.FacturaService;
import com.acueducto.backend.service.TesoreriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "Consultar estado del servicio por documento",
            description = "Consulta publica minima: confirma si el documento esta registrado y el estado del servicio.")
    @GetMapping("/estado-servicio")
    public ResponseEntity<EstadoServicioPublicoResponse> consultarEstadoServicio(@RequestParam String documento) {
        Asociado asociado = asociadoRepository.findByDocumento(documento)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro un asociado con ese documento."));

        return ResponseEntity.ok(new EstadoServicioPublicoResponse(
                asociado.getCodigoInterno(),
                asociado.getNombres() + " " + asociado.getApellidos(),
                asociado.getEstadoServicio()
        ));
    }

    @Getter
    @Setter
    public static class EstadoServicioPublicoResponse {
        private String codigoInterno;
        private String nombreCompleto;
        private EstadoServicio estadoServicio;

        public EstadoServicioPublicoResponse(String codigoInterno, String nombreCompleto, EstadoServicio estadoServicio) {
            this.codigoInterno = codigoInterno;
            this.nombreCompleto = nombreCompleto;
            this.estadoServicio = estadoServicio;
        }
    }
}
