package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Factura de cobro generada a partir de una lectura. Una factura solo puede generarse
 * una vez por asociado, mes y anio (7.13).
 */
@Entity
@Table(name = "facturas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_factura_numero", columnNames = "numero_factura"),
        @UniqueConstraint(name = "uk_factura_asociado_mes", columnNames = {"asociado_id", "mes_contable_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura extends BaseEntity {

    @Column(name = "numero_factura", nullable = false, length = 20)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asociado_id", nullable = false)
    private Asociado asociado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lectura_id")
    private Lectura lectura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mes_contable_id", nullable = false)
    private MesContable mesContable;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_limite_pago", nullable = false)
    private LocalDate fechaLimitePago;

    @Column(name = "lectura_anterior")
    private Integer lecturaAnterior;

    @Column(name = "lectura_actual")
    private Integer lecturaActual;

    @Column(name = "consumo_m3")
    private Integer consumoM3;

    @Column(name = "valor_m3", precision = 12, scale = 2)
    private BigDecimal valorM3;

    @Column(name = "valor_consumo", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorConsumo;

    @Column(name = "cargo_administracion", nullable = false, precision = 12, scale = 2)
    private BigDecimal cargoAdministracion;

    @Builder.Default
    @Column(name = "valores_adicionales", nullable = false, precision = 12, scale = 2)
    private BigDecimal valoresAdicionales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_multas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalMultas = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @Column(name = "total_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPagado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    @Column(name = "codigo_qr", columnDefinition = "TEXT")
    private String codigoQr;

    @Builder.Default
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConceptoFactura> conceptos = new ArrayList<>();

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    /** Saldo pendiente por pagar; se usa para bloquear pagos duplicados o mayores al saldo (4.12). */
    @Transient
    public BigDecimal getSaldoPendiente() {
        if (total == null) return BigDecimal.ZERO;
        BigDecimal pagado = totalPagado == null ? BigDecimal.ZERO : totalPagado;
        return total.subtract(pagado);
    }
}
