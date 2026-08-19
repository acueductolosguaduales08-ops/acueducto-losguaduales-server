package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.SolicitudEliminacion;
import com.acueducto.backend.entity.enums.EstadoSolicitudEliminacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de eliminacion de mensaje. `rolUsuario` indica la perspectiva del usuario
 * autenticado (CONFIRMADOR para aceptar/rechazar, SOLICITANTE para cancelar).
 * `mensajeEliminado` se marca en true cuando la solicitud fue aceptada y el mensaje
 * ya fue borrado del backend (el frontend debe eliminarlo tambien de IndexedDB).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudEliminacionResponse {

    private Long id;
    private Long mensajeId;
    private String contenido;
    private ParticipanteResponse solicitante;
    private ParticipanteResponse confirmador;
    private EstadoSolicitudEliminacion estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String rolUsuario;
    private boolean mensajeEliminado;

    public static SolicitudEliminacionResponse fromEntity(SolicitudEliminacion s, Long usuarioActualId) {
        boolean esConfirmador = s.getConfirmador().getId().equals(usuarioActualId);
        return SolicitudEliminacionResponse.builder()
                .id(s.getId())
                .mensajeId(s.getMensaje().getId())
                .contenido(s.getMensaje().getContenido())
                .solicitante(ParticipanteResponse.fromUsuario(s.getSolicitante()))
                .confirmador(ParticipanteResponse.fromUsuario(s.getConfirmador()))
                .estado(s.getEstado())
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaResolucion(s.getFechaResolucion())
                .rolUsuario(esConfirmador ? "CONFIRMADOR" : "SOLICITANTE")
                .mensajeEliminado(false)
                .build();
    }
}