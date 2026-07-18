package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "respuestas_encuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaEncuesta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encuesta_id", nullable = false)
    private Encuesta encuesta;

    /** Nulo cuando la respuesta es anonima o el respondiente es un usuario publico. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Nombre digitado por un respondiente publico (sin sesion) cuando el formulario NO es anonimo.
     * Queda nulo si el formulario es anonimo o si quien respondio lo hizo con una cuenta (usuario != null).
     */
    @Column(name = "nombre_respondiente", length = 150)
    private String nombreRespondiente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 45)
    private String ip;

    @Builder.Default
    @OneToMany(mappedBy = "respuestaEncuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaPregunta> respuestas = new ArrayList<>();
}
