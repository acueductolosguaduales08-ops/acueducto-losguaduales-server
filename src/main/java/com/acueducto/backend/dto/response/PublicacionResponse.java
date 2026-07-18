package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Publicacion;
import com.acueducto.backend.entity.enums.EstadoPublicacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionResponse {
    private Long id;
    private String titulo;
    private String descripcionCorta;
    private String contenidoCompleto;
    private String imagenUrl;
    private String posicionImagen;
    private String categoria;
    private List<String> etiquetas;
    private String autor;
    private EstadoPublicacion estado;
    private boolean destacada;
    private LocalDateTime fechaCreacion;

    public static PublicacionResponse fromEntity(Publicacion p) {
        return PublicacionResponse.builder()
                .id(p.getId()).titulo(p.getTitulo()).descripcionCorta(p.getDescripcionCorta())
                .contenidoCompleto(p.getContenidoCompleto()).imagenUrl(p.getImagenUrl())
                .posicionImagen(p.getPosicionImagen())
                .categoria(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .etiquetas(p.getEtiquetas().stream().map(com.acueducto.backend.entity.Etiqueta::getNombre).toList())
                .autor(p.getAutor().getUsername())
                .estado(p.getEstado()).destacada(p.isDestacada())
                .fechaCreacion(p.getFechaCreacion())
                .build();
    }
}
