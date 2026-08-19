package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo para enviar un mensaje de chat. Solo texto y emojis. */
public record CrearMensajeRequest(
        @NotBlank(message = "El mensaje no puede estar vacio")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String contenido
) {
}