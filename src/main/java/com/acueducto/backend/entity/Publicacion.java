package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoPublicacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Publicacion del Portal Publico: galeria, destacados y contenido enriquecido (Modulo 11).
 * El campo destacada determina si aparece en la seccion de publicaciones destacadas.
 */
@Entity
@Table(name = "publicaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicacion extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion_corta", length = 500)
    private String descripcionCorta;

    @Column(name = "contenido_completo", columnDefinition = "TEXT")
    private String contenidoCompleto; // HTML enriquecido

    // Sin limite de longitud: las URLs de imagenes (CDN, enlaces firmados con querystring largo, etc.)
    // superan facilmente los 300 caracteres que tenia antes y provocaban un 500 al guardar.
    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "posicion_imagen", length = 15)
    private String posicionImagen; // SUPERIOR | CENTRO | INFERIOR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToMany
    @JoinTable(name = "publicaciones_etiquetas",
            joinColumns = @JoinColumn(name = "publicacion_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    @Builder.Default
    private Set<Etiqueta> etiquetas = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 15)
    private EstadoPublicacion estado = EstadoPublicacion.BORRADOR;

    @Builder.Default
    @Column(nullable = false)
    private boolean destacada = false;
}
