package com.acueducto.backend.dto.request;

import com.acueducto.backend.entity.enums.PrioridadNotificacion;
import com.acueducto.backend.entity.enums.TipoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NotificacionRequest(
        @NotBlank @Size(max = 200, message = "El titulo no puede superar 200 caracteres") String titulo,
        @Size(max = 500, message = "La descripcion corta no puede superar 500 caracteres") String descripcionCorta,
        String contenidoCompleto,
        @NotNull TipoNotificacion tipo,
        PrioridadNotificacion prioridad,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaVencimiento,
        Long destinatarioId,
        String enlaceUrl
) {
}
