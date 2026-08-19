package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.MesContable;
import com.acueducto.backend.entity.enums.EstadoMes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesContableResponse {
    private Long id;
    private String nombreMes;
    private Integer numeroMes;
    private Integer anio;
    private EstadoMes estado;
    private LocalDate fechaApertura;
    private LocalDate fechaCierre;

    public static MesContableResponse fromEntity(MesContable m) {
        return MesContableResponse.builder()
                .id(m.getId())
                .nombreMes(m.getNombreMes())
                .numeroMes(m.getNumeroMes())
                .anio(m.getAnioContable().getAnio())
                .estado(m.getEstado())
                .fechaApertura(m.getFechaApertura())
                .fechaCierre(m.getFechaCierre())
                .build();
    }
}
