package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoRecibo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recibos", uniqueConstraints = @UniqueConstraint(name = "uk_recibo_numero", columnNames = "numero_recibo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recibo extends BaseEntity {

    @Column(name = "numero_recibo", nullable = false, length = 20)
    private String numeroRecibo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asociado_id", nullable = false)
    private Asociado asociado;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "saldo_pendiente", precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "codigo_qr", columnDefinition = "TEXT")
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoRecibo estado = EstadoRecibo.EMITIDO;
}
