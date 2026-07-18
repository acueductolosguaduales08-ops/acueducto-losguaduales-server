package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conteo publico de reacciones (emojis) sobre una publicacion/imagen de la galeria (Modulo 11).
 * No se guarda quien reacciono: solo un contador total por cada emoji distinto usado en esa
 * publicacion. La prevencion de doble clic (que un mismo dispositivo reaccione varias veces)
 * es responsabilidad del frontend, guardando localmente que ya reacciono.
 */
@Entity
@Table(name = "reacciones_publicacion", uniqueConstraints = @UniqueConstraint(
        name = "uk_reaccion_publicacion_emoji", columnNames = {"publicacion_id", "emoji"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReaccionPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    /** El emoji tal cual (ej. "\uD83D\uDC4D"), o un codigo corto equivalente definido por el frontend. */
    @Column(nullable = false, length = 16)
    private String emoji;

    @Builder.Default
    @Column(nullable = false)
    private long contador = 0L;
}
