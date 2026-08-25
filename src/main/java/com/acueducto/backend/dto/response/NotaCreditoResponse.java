package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.NotaCredito;
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
public class NotaCreditoResponse {
    private Long id;
    private String numeroNota;
    private Long asociadoId;
    private String asociadoNombre;
    private Long facturaId;
    private String numeroFactura;
    private String motivo;
    private BigDecimal valor;
    private LocalDate fechaEmision;
    private String estado;
    private String observaciones;

    public static NotaCreditoResponse fromEntity(NotaCredito nc) {
        return NotaCreditoResponse.builder()
                .id(nc.getId())
                .numeroNota(nc.getNumeroNota())
                .asociadoId(nc.getAsociado().getId())
                .asociadoNombre(nc.getAsociado().getNombres() + " " + nc.getAsociado().getApellidos())
                .facturaId(nc.getFactura() != null ? nc.getFactura().getId() : null)
                .numeroFactura(nc.getFactura() != null ? nc.getFactura().getNumeroFactura() : null)
                .motivo(nc.getMotivo())
                .valor(nc.getValor())
                .fechaEmision(nc.getFechaEmision())
                .estado(nc.getEstado().name())
                .observaciones(nc.getObservaciones())
                .build();
    }
}
