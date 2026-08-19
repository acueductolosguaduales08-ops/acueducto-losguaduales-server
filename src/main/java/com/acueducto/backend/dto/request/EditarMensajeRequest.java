package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo para editar un mensaje de chat (solo el autor). */
public record EditarMensajeRequest(
        @NotBlank(message = "El mensaje no puede estar vacio")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String contenido
) {
}