package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.MovimientoBancario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoBancarioResponse {
    private Long id;
    private LocalDate fechaTransaccion;
    private String descripcion;
    private BigDecimal valor;
    private String numeroReferencia;
    private String estado;
    private Long movimientoTesoreriaId;
    private String archivoOrigen;
    private String observaciones;

    public static MovimientoBancarioResponse fromEntity(MovimientoBancario mb) {
        return MovimientoBancarioResponse.builder()
                .id(mb.getId())
                .fechaTransaccion(mb.getFechaTransaccion())
                .descripcion(mb.getDescripcion())
                .valor(mb.getValor())
                .numeroReferencia(mb.getNumeroReferencia())
                .estado(mb.getEstado().name())
                .movimientoTesoreriaId(mb.getMovimientoTesoreria() != null ? mb.getMovimientoTesoreria().getId() : null)
                .archivoOrigen(mb.getArchivoOrigen())
                .observaciones(mb.getObservaciones())
                .build();
    }
}
