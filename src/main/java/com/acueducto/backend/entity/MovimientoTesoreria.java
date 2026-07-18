package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada o salida de dinero del acueducto (Modulo 8). Los pagos de facturas generan
 * automaticamente una entrada; los gastos administrativos se registran como salida.
 */
@Entity
@Table(name = "movimientos_tesoreria", uniqueConstraints = @UniqueConstraint(
        name = "uk_movimiento_numero_tipo", columnNames = {"numero", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoTesoreria extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Long numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(length = 60)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asociado_id")
    private Asociado asociado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibo_id")
    private Recibo recibo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mes_contable_id", nullable = false)
    private MesContable mesContable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "comprobante_url", columnDefinition = "TEXT")
    private String comprobanteUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean anulado = false;
}
