package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Informe financiero y administrativo de un periodo (un mes o un anio completo).
 * Pensado para que el Tesorero o el Administrador lo presenten como reporte oficial,
 * en HTML o en PDF, con la misma informacion y los mismos logos institucionales que
 * facturas y recibos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformePeriodoResponse {

    private String tipoInforme;      // "Mensual" o "Anual"
    private String tituloPeriodo;    // ej. "Julio 2026" o "Año 2026"
    private LocalDate fechaGeneracion;

    // ---- Tesoreria ----
    private BigDecimal totalIngresos;
    private BigDecimal totalGastos;
    private BigDecimal balance;

    // ---- Facturacion ----
    private long facturasGeneradas;
    private long facturasPagadas;
    private long facturasPendientes;
    private long facturasVencidas;
    private long facturasAnuladas;
    private BigDecimal totalFacturado;
    private BigDecimal totalRecaudadoFacturas;

    // ---- Multas ----
    private long numeroMultas;
    private BigDecimal totalMultas;

    // ---- Consumo ----
    private long totalM3Consumidos;
    private double promedioConsumoM3;

    // ---- Asociados ----
    private long asociadosActivos;
    private long asociadosSuspendidos;

    private List<FacturaResumenItem> facturas;
    private List<MovimientoResumenItem> movimientos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaResumenItem {
        private String numeroFactura;
        private String asociadoNombre;
        private LocalDate fechaEmision;
        private BigDecimal total;
        private String estado;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimientoResumenItem {
        private String numero;
        private String tipo;
        private String concepto;
        private BigDecimal valor;
        private LocalDate fecha;
    }
}
