package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Representa el consumo registrado en un periodo especifico para un medidor (6.5). */
@Entity
@Table(name = "lecturas", uniqueConstraints = @UniqueConstraint(
        name = "uk_lectura_medidor_mes", columnNames = {"medidor_id", "mes_contable_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lectura extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asociado_id", nullable = false)
    private Asociado asociado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medidor_id", nullable = false)
    private Medidor medidor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mes_contable_id", nullable = false)
    private MesContable mesContable;

    @Column(name = "fecha_lectura", nullable = false)
    private LocalDate fechaLectura;

    @Column(name = "lectura_anterior", nullable = false)
    private Integer lecturaAnterior;

    @Column(name = "lectura_actual", nullable = false)
    private Integer lecturaActual;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Builder.Default
    @Column(name = "factura_generada", nullable = false)
    private boolean facturaGenerada = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean anulada = false;

    /** Consumo (m3) = lectura actual - lectura anterior. Nunca sera negativo (6.6). */
    @Transient
    public int getConsumoM3() {
        return lecturaActual - lecturaAnterior;
    }
}
