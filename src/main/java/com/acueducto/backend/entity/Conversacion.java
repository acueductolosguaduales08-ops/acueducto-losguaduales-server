package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Conversacion de chat entre dos usuarios (Asociado <-> Admin/Tesorero, o entre
 * administrativos). Los participantes se guardan normalizados (id menor en usuario1)
 * para garantizar una sola conversacion por pareja de usuarios.
 */
@Entity
@Table(name = "conversaciones", indexes = {
        @Index(name = "idx_conv_usuario1", columnList = "usuario1_id"),
        @Index(name = "idx_conv_usuario2", columnList = "usuario2_id")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_conversacion_participantes", columnNames = {"usuario1_id", "usuario2_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario1_id", nullable = false)
    private Usuario usuario1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario2_id", nullable = false)
    private Usuario usuario2;

    @Builder.Default
    @Column(nullable = false)
    private boolean activa = true;
}