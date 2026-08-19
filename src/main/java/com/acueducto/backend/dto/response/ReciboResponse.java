package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.entity.enums.EstadoRecibo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReciboResponse {
    private Long id;
    private String numeroRecibo;
    private String numeroFactura;
    private Long asociadoId;
    private String asociadoNombre;
    private LocalDateTime fechaEmision;
    private BigDecimal valor;
    private BigDecimal saldoPendiente;
    private String metodoPago;
    private EstadoRecibo estado;
    private String codigoQr;

    public static ReciboResponse fromEntity(Recibo r) {
        return ReciboResponse.builder()
                .id(r.getId())
                .numeroRecibo(r.getNumeroRecibo())
                .numeroFactura(r.getFactura().getNumeroFactura())
                .asociadoId(r.getAsociado().getId())
                .asociadoNombre(r.getAsociado().getNombres() + " " + r.getAsociado().getApellidos())
                .fechaEmision(r.getFechaEmision())
                .valor(r.getValor())
                .saldoPendiente(r.getSaldoPendiente())
                .metodoPago(r.getPago().getMetodoPago().getNombre())
                .estado(r.getEstado())
                .codigoQr(r.getCodigoQr())
                .build();
    }
}
