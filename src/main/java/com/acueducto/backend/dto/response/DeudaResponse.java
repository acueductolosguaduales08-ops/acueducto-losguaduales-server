package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeudaResponse {
    private long totalFacturasVencidas;
    private BigDecimal montoTotalVencido;
    private List<GrupoAging> aging;
    private List<DetalleDeuda> detalle;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrupoAging {
        private String rango;
        private long cantidad;
        private BigDecimal monto;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleDeuda {
        private Long facturaId;
        private String numeroFactura;
        private Long asociadoId;
        private String asociadoNombre;
        private String numeroMedidor;
        private BigDecimal total;
        private BigDecimal totalPagado;
        private BigDecimal saldoPendiente;
        private String fechaLimitePago;
        private long diasVencimiento;
        private String rangoAging;
    }
}
