package com.acueducto.backend.dto.response;

import com.acueducto.backend.entity.Encuesta;
import com.acueducto.backend.entity.PreguntaEncuesta;
import com.acueducto.backend.entity.enums.EstadoEncuesta;
import com.acueducto.backend.entity.enums.TipoPregunta;
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
public class EncuestaResponse {
    private Long id;
    private String codigo;
    private String titulo;
    private String descripcion;
    private EstadoEncuesta estado;
    private boolean publico;
    private boolean requiereAutenticacion;
    private boolean respuestaUnica;
    private boolean respuestasAnonimas;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String codigoQr;
    private List<PreguntaResponse> preguntas;

    public static EncuestaResponse fromEntity(Encuesta e) {
        return EncuestaResponse.builder()
                .id(e.getId()).codigo(e.getCodigo()).titulo(e.getTitulo()).descripcion(e.getDescripcion())
                .estado(e.getEstado()).publico(e.isPublico()).requiereAutenticacion(e.isRequiereAutenticacion())
                .respuestaUnica(e.isRespuestaUnica()).respuestasAnonimas(e.isRespuestasAnonimas())
                .fechaInicio(e.getFechaInicio()).fechaFin(e.getFechaFin())
                .codigoQr(e.getCodigoQr())
                .preguntas(e.getPreguntas().stream().map(PreguntaResponse::fromEntity).toList())
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreguntaResponse {
        private Long id;
        private String texto;
        private TipoPregunta tipo;
        private boolean obligatoria;
        private Integer orden;
        private List<String> opciones;

        public static PreguntaResponse fromEntity(PreguntaEncuesta p) {
            return PreguntaResponse.builder()
                    .id(p.getId()).texto(p.getTexto()).tipo(p.getTipo())
                    .obligatoria(p.isObligatoria()).orden(p.getOrden())
                    .opciones(p.getOpciones() != null ? List.of(p.getOpciones().split("\\|")) : List.of())
                    .build();
        }
    }
}
