package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoMulta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "multas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Multa extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asociado_id", nullable = false)
    private Asociado asociado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoMulta estado = EstadoMulta.PENDIENTE;

    /**
     * Multa "aparte": no tiene nada que ver con facturas ni recibos, se paga de forma directa
     * (ver TesoreriaService.pagarMultaIndependiente) y nunca se incluye automaticamente en la
     * siguiente factura del asociado (a diferencia de una multa regular).
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean independiente = false;
}
