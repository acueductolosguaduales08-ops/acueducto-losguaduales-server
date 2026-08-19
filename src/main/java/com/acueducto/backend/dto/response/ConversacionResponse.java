package com.acueducto.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Resumen de una conversacion para la lista de conversaciones del usuario autenticado. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionResponse {

    private Long id;
    private ParticipanteResponse participante;
    private String ultimoMensaje;
    private LocalDateTime fechaUltimoMensaje;
    private long noLeidos;
    private boolean solicitudPendienteEnviada;
    private boolean solicitudPendienteRecibida;
}