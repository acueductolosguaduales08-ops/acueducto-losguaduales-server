package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "etiquetas", uniqueConstraints = @UniqueConstraint(name = "uk_etiqueta_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiqueta extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 10)
    private String color;
}
