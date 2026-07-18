package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoEncuesta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Formulario/encuesta dinamica (Modulo 12). Cada formulario tiene URL y QR unicos (12.13). */
@Entity
@Table(name = "encuestas", uniqueConstraints = @UniqueConstraint(name = "uk_encuesta_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encuesta extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String codigo; // p.ej. FRM-000025

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoEncuesta estado = EstadoEncuesta.BORRADOR;

    @Builder.Default
    @Column(nullable = false)
    private boolean publico = true;

    @Builder.Default
    @Column(name = "requiere_autenticacion", nullable = false)
    private boolean requiereAutenticacion = false;

    @Builder.Default
    @Column(name = "respuesta_unica", nullable = false)
    private boolean respuestaUnica = true;

    @Builder.Default
    @Column(name = "respuestas_anonimas", nullable = false)
    private boolean respuestasAnonimas = false;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "codigo_qr", columnDefinition = "TEXT")
    private String codigoQr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Builder.Default
    @OneToMany(mappedBy = "encuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PreguntaEncuesta> preguntas = new ArrayList<>();
}
