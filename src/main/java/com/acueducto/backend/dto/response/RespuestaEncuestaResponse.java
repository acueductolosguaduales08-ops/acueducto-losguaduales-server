package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.RespuestaEncuesta;
import com.acueducto.backend.entity.RespuestaPregunta;
import com.acueducto.backend.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEncuestaResponse {
    private Long id;
    /** Nombre de quien respondio, o "Anonimo" cuando el formulario es anonimo o no se identifico. */
    private String nombreRespondiente;
    private LocalDateTime fecha;
    private List<ItemRespuesta> respuestas;

    public static RespuestaEncuestaResponse fromEntity(RespuestaEncuesta r) {
        return RespuestaEncuestaResponse.builder()
                .id(r.getId())
                .nombreRespondiente(resolverNombre(r))
                .fecha(r.getFecha())
                .respuestas(r.getRespuestas().stream().map(ItemRespuesta::fromEntity).toList())
                .build();
    }

    private static String resolverNombre(RespuestaEncuesta r) {
        Usuario usuario = r.getUsuario();
        if (usuario != null) {
            if (usuario.getAsociado() != null) {
                return usuario.getAsociado().getNombres() + " " + usuario.getAsociado().getApellidos();
            }
            return usuario.getUsername();
        }
        if (r.getNombreRespondiente() != null && !r.getNombreRespondiente().isBlank()) {
            return r.getNombreRespondiente();
        }
        return "Anonimo";
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRespuesta {
        private Long preguntaId;
        private String pregunta;
        private String valor;

        public static ItemRespuesta fromEntity(RespuestaPregunta rp) {
            return ItemRespuesta.builder()
                    .preguntaId(rp.getPregunta().getId())
                    .pregunta(rp.getPregunta().getTexto())
                    .valor(rp.getValor())
                    .build();
        }
    }
}
