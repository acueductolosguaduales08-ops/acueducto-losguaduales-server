package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.EstadoSolicitudEliminacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de eliminacion de un mensaje de chat.
 *
 * La eliminacion de mensajes NUNCA es directa: el autor solicita eliminar su mensaje
 * y el otro participante debe aceptarlo. Mientras tanto el mensaje permanece visible.
 *
 * Estados: PENDIENTE -> ACEPTADA (elimina el mensaje) | RECHAZADA (se conserva) |
 * CANCELADA (la cancela el mismo solicitante).
 */
@Entity
@Table(name = "solicitudes_eliminacion", indexes = {
        @Index(name = "idx_sol_mensaje", columnList = "mensaje_id"),
        @Index(name = "idx_sol_solicitante_estado", columnList = "solicitante_id,estado"),
        @Index(name = "idx_sol_confirmador_estado", columnList = "confirmador_id,estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEliminacion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmador_id", nullable = false)
    private Usuario confirmador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitudEliminacion estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}