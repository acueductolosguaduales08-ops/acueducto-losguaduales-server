package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Informe de seguimiento a un asociado especifico: su historial de facturas, pagos
 * y multas, pensado para que el Tesorero o el Administrador lo presenten como
 * reporte oficial en HTML o PDF, con los mismos logos institucionales que facturas y recibos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeAsociadoResponse {

    private LocalDate fechaGeneracion;

    // ---- Datos del asociado ----
    private String codigoInterno;
    private String documento;
    private String nombreCompleto;
    private String telefonoPrincipal;
    private String direccion;
    private String estadoServicio;
    private LocalDate fechaAfiliacion;
    private String numeroMedidor;

    // ---- Resumen financiero ----
    private long totalFacturas;
    private long facturasPagadas;
    private long facturasPendientes;
    private long facturasVencidas;
    private BigDecimal totalFacturado;
    private BigDecimal totalPagado;
    private BigDecimal totalPendiente;
    private long numeroMultas;
    private BigDecimal totalMultas;

    private List<FacturaResumenItem> facturas;
    private List<PagoResumenItem> pagos;
    private List<MultaResumenItem> multas;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaResumenItem {
        private String numeroFactura;
        private LocalDate fechaEmision;
        private BigDecimal total;
        private BigDecimal saldoPendiente;
        private String estado;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagoResumenItem {
        private String numeroRecibo;
        private String numeroFactura;
        private BigDecimal valor;
        private LocalDateTime fecha;
        private String metodoPago;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MultaResumenItem {
        private String motivo;
        private BigDecimal valor;
        private LocalDate fecha;
        private String estado;
    }
}
