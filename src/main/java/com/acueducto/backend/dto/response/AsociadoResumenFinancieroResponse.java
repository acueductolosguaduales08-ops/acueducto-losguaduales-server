package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Informacion financiera calculada dinamicamente (5.4): nunca se almacena como campo independiente. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsociadoResumenFinancieroResponse {
    private long totalFacturas;
    private long facturasPagadas;
    private long facturasPendientes;
    private BigDecimal totalPagado;
    private BigDecimal totalPendiente;
    private long numeroMultas;
    private BigDecimal totalMultas;
}
