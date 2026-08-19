package com.acueducto.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** Cuerpo para crear (u obtener) una conversacion de chat indicando el usuario destinatario. */
public record CrearConversacionRequest(
        @NotNull(message = "Debe indicar el usuario destinatario") Long destinatarioId
) {
}