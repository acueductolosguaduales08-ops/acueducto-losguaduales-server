package com.acueducto.backend.entity.enums;

/**
 * Estado de una solicitud de eliminacion de mensaje en el chat.
 * La eliminacion de mensajes nunca es directa: requiere que el autor la solicite
 * y que el otro participante la acepte. CANCELADA solo puede hacerla el solicitante.
 */
public enum EstadoSolicitudEliminacion {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA
}