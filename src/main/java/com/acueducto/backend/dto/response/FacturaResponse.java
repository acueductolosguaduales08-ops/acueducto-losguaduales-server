package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoFactura;
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
public class FacturaResponse {
    private Long id;
    private String numeroFactura;
    private Long asociadoId;
    private String asociadoNombre;
    private String numeroMedidor;
    private LocalDate fechaEmision;
    private LocalDate fechaLimitePago;
    private Integer lecturaAnterior;
    private Integer lecturaActual;
    private Integer consumoM3;
    private BigDecimal valorConsumo;
    private BigDecimal cargoAdministracion;
    private BigDecimal valoresAdicionales;
    private BigDecimal totalMultas;
    private BigDecimal total;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private EstadoFactura estado;
    private String codigoQr;

    public static FacturaResponse fromEntity(Factura f) {
        return FacturaResponse.builder()
                .id(f.getId())
                .numeroFactura(f.getNumeroFactura())
                .asociadoId(f.getAsociado().getId())
                .asociadoNombre(f.getAsociado().getNombres() + " " + f.getAsociado().getApellidos())
                .numeroMedidor(f.getAsociado().getMedidor() != null ? f.getAsociado().getMedidor().getNumero() : null)
                .fechaEmision(f.getFechaEmision())
                .fechaLimitePago(f.getFechaLimitePago())
                .lecturaAnterior(f.getLecturaAnterior())
                .lecturaActual(f.getLecturaActual())
                .consumoM3(f.getConsumoM3())
                .valorConsumo(f.getValorConsumo())
                .cargoAdministracion(f.getCargoAdministracion())
                .valoresAdicionales(f.getValoresAdicionales())
                .totalMultas(f.getTotalMultas())
                .total(f.getTotal())
                .totalPagado(f.getTotalPagado())
                .saldoPendiente(f.getSaldoPendiente())
                .estado(f.getEstado())
                .codigoQr(f.getCodigoQr())
                .build();
    }
}
