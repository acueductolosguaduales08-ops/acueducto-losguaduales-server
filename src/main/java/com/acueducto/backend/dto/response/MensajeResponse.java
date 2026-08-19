package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Mensaje;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Mensaje de chat listo para exponer al frontend. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {

    private Long id;
    private Long conversacionId;
    private ParticipanteResponse remitente;
    private String contenido;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private boolean editado;
    private boolean leido;
    private LocalDateTime fechaLectura;

    public static MensajeResponse fromEntity(Mensaje m) {
        return MensajeResponse.builder()
                .id(m.getId())
                .conversacionId(m.getConversacion().getId())
                .remitente(ParticipanteResponse.fromUsuario(m.getRemitente()))
                .contenido(m.getContenido())
                .fechaCreacion(m.getFechaCreacion())
                .fechaModificacion(m.getFechaModificacion())
                .editado(m.isEditado())
                .leido(m.isLeido())
                .fechaLectura(m.getFechaLectura())
                .build();
    }
}