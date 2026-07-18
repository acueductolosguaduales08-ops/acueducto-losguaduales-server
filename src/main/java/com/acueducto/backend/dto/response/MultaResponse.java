package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.entity.enums.EstadoMulta;
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
public class MultaResponse {
    private Long id;
    private Long asociadoId;
    private String motivo;
    private BigDecimal valor;
    private LocalDate fecha;
    private EstadoMulta estado;

    public static MultaResponse fromEntity(Multa m) {
        return MultaResponse.builder()
                .id(m.getId())
                .asociadoId(m.getAsociado().getId())
                .motivo(m.getMotivo())
                .valor(m.getValor())
                .fecha(m.getFecha())
                .estado(m.getEstado())
                .build();
    }
}
