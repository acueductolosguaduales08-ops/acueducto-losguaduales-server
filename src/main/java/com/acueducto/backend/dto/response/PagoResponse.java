package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Pago;
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
public class PagoResponse {
    private Long id;
    private String numeroFactura;
    private Long asociadoId;
    private BigDecimal valor;
    private LocalDateTime fecha;
    private String metodoPago;
    private String tesorero;
    private String numeroRecibo;

    public static PagoResponse fromEntity(Pago p, String numeroRecibo) {
        return PagoResponse.builder()
                .id(p.getId())
                .numeroFactura(p.getFactura().getNumeroFactura())
                .asociadoId(p.getAsociado().getId())
                .valor(p.getValor())
                .fecha(p.getFecha())
                .metodoPago(p.getMetodoPago().getNombre())
                .tesorero(p.getTesorero().getUsername())
                .numeroRecibo(numeroRecibo)
                .build();
    }
}
