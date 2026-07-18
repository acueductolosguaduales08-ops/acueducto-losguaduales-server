package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.AnioContable;
import com.acueducto.backend.entity.enums.EstadoAnio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnioContableResponse {
    private Long id;
    private Integer anio;
    private EstadoAnio estado;

    public static AnioContableResponse fromEntity(AnioContable a) {
        return AnioContableResponse.builder().id(a.getId()).anio(a.getAnio()).estado(a.getEstado()).build();
    }
}
