package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String username;
    private String email;
    private Rol rol;
    private boolean activo;
    private Long asociadoId;

    public static UsuarioResponse fromEntity(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .rol(u.getRol())
                .activo(u.isActivo())
                .asociadoId(u.getAsociado() != null ? u.getAsociado().getId() : null)
                .build();
    }
}
