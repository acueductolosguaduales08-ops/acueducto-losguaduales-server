package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoMes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Cada mes pertenece a un anio contable y controla la apertura/cierre de operaciones (9.5). */
@Entity
@Table(name = "meses_contables", uniqueConstraints = @UniqueConstraint(
        name = "uk_mes_anio", columnNames = {"numero_mes", "anio_contable_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesContable extends BaseEntity {

    @Column(name = "nombre_mes", nullable = false, length = 20)
    private String nombreMes;

    @Column(name = "numero_mes", nullable = false)
    private Integer numeroMes; // 1-12

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anio_contable_id", nullable = false)
    private AnioContable anioContable;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoMes estado = EstadoMes.ABIERTO;

    @Column(name = "fecha_apertura")
    private LocalDate fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;
}
