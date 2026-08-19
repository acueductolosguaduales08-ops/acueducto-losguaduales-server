package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

/** Informacion de un participante de chat: nombre visible, iniciales y rol. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteResponse {

    private Long id;
    private String nombre;
    private String iniciales;
    private Rol rol;

    public static ParticipanteResponse fromUsuario(Usuario u) {
        String nombre = nombreVisible(u);
        return ParticipanteResponse.builder()
                .id(u.getId())
                .nombre(nombre)
                .iniciales(inicialesDe(nombre))
                .rol(u.getRol())
                .build();
    }

    /** Para un Asociado usa los nombres reales del expediente; para Admin/Tesorero usa el username. */
    private static String nombreVisible(Usuario u) {
        if (u.getAsociado() != null && u.getAsociado().getNombres() != null && u.getAsociado().getApellidos() != null) {
            return u.getAsociado().getNombres() + " " + u.getAsociado().getApellidos();
        }
        return u.getUsername();
    }

    private static String inicialesDe(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isBlank()) {
                sb.append(parte.charAt(0));
            }
        }
        String iniciales = sb.toString().toUpperCase(Locale.ROOT);
        return iniciales.length() >= 2 ? iniciales.substring(0, 2) : iniciales;
    }
}