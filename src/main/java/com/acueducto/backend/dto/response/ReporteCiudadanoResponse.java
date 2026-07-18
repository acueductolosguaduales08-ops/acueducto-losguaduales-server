package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.ReporteCiudadano;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCiudadanoResponse {
    private Long id;
    private String nombre;
    private String mensaje;
    private String contacto;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEliminacion;

    public static ReporteCiudadanoResponse fromEntity(ReporteCiudadano r) {
        return ReporteCiudadanoResponse.builder()
                .id(r.getId())
                .nombre(r.getNombre())
                .mensaje(r.getMensaje())
                .contacto(r.getContacto())
                .fechaCreacion(r.getFechaCreacion())
                .fechaEliminacion(r.getFechaEliminacion())
                .build();
    }
}
