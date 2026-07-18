package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Auditoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponse {
    private Long id;
    private String usuario;
    private LocalDateTime fecha;
    private String accion;
    private String modulo;
    private String registroAfectado;
    private String observaciones;

    public static AuditoriaResponse fromEntity(Auditoria a) {
        return AuditoriaResponse.builder()
                .id(a.getId()).usuario(a.getUsuario()).fecha(a.getFecha()).accion(a.getAccion())
                .modulo(a.getModulo()).registroAfectado(a.getRegistroAfectado())
                .observaciones(a.getObservaciones())
                .build();
    }
}
