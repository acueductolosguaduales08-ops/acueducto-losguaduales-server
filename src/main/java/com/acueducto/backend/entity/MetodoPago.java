package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Metodos de pago configurables sin modificar codigo (8.7 / 10.7). */
@Entity
@Table(name = "metodos_pago", uniqueConstraints = @UniqueConstraint(name = "uk_metodo_pago_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPago extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
