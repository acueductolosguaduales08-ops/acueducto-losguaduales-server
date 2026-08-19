package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Resumen automatico de un periodo contable (9.9). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenPeriodoResponse {
    private long facturasGeneradas;
    private long facturasPagadas;
    private long facturasPendientes;
    private long facturasVencidas;
    private BigDecimal totalIngresos;
    private BigDecimal totalGastos;
    private BigDecimal balance;
    private long totalM3Consumidos;
    private double promedioConsumo;
    private long asociadosActivos;
    private long asociadosSuspendidos;
}
