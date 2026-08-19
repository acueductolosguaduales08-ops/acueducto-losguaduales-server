package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.TipoPregunta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preguntas_encuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaEncuesta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encuesta_id", nullable = false)
    private Encuesta encuesta;

    @Column(nullable = false, length = 300)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPregunta tipo;

    @Builder.Default
    @Column(nullable = false)
    private boolean obligatoria = true;

    @Column(nullable = false)
    private Integer orden;

    /** Opciones separadas por "|" para preguntas de opcion unica/multiple. */
    @Column(columnDefinition = "TEXT")
    private String opciones;

    @Builder.Default
    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaPregunta> respuestas = new ArrayList<>();
}
