package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublicacionRequest(
        @NotBlank @Size(max = 200, message = "El titulo no puede superar 200 caracteres") String titulo,
        @Size(max = 500, message = "La descripcion corta no puede superar 500 caracteres") String descripcionCorta,
        String contenidoCompleto,
        String imagenUrl,
        @Size(max = 15, message = "La posicion de imagen no es valida") String posicionImagen,
        Long categoriaId,
        List<Long> etiquetasIds,
        boolean destacada
) {
}
