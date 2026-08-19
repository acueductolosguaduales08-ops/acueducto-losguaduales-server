package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoAnio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Solo puede existir un anio ACTIVO para registrar operaciones (9.4). */
@Entity
@Table(name = "anios_contables", uniqueConstraints = @UniqueConstraint(name = "uk_anio", columnNames = "anio"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnioContable extends BaseEntity {

    @Column(nullable = false)
    private Integer anio;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoAnio estado = EstadoAnio.ACTIVO;

    @Builder.Default
    @OneToMany(mappedBy = "anioContable")
    private List<MesContable> meses = new ArrayList<>();
}
