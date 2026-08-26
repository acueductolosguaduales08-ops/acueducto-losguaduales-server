package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultaEstadisticasResponse {
    private long totalMultas;
    private long pendientes;
    private long pagadas;
    private long anuladas;
    private BigDecimal totalValorPendiente;
}
