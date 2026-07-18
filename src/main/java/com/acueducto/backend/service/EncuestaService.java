package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.EncuestaRequest;
import com.acueducto.backend.dto.request.PreguntaEncuestaRequest;
import com.acueducto.backend.dto.request.ResponderEncuestaRequest;
import com.acueducto.backend.dto.response.EncuestaEstadisticasResponse;
import com.acueducto.backend.dto.response.EncuestaResponse;
import com.acueducto.backend.dto.response.RespuestaEncuestaResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoEncuesta;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import com.acueducto.backend.util.NumeracionUtil;
import com.acueducto.backend.util.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modulo de Encuestas y Formularios Dinamicos (Modulo 12). Cada formulario tiene URL y
 * QR unicos; los formularios cerrados o archivados no aceptan nuevas respuestas (12.13).
 */
@Service
@RequiredArgsConstructor
public class EncuestaService {

    private final EncuestaRepository encuestaRepository;
    private final PreguntaEncuestaRepository preguntaEncuestaRepository;
    private final RespuestaEncuestaRepository respuestaEncuestaRepository;
    private final RespuestaPreguntaRepository respuestaPreguntaRepository;
    private final UsuarioRepository usuarioRepository;
    private final QrCodeService qrCodeService;
    private final AuditoriaService auditoriaService;

    private static long contadorFormularios = 1;

    @Transactional
    public EncuestaResponse crear(EncuestaRequest request, String autorUsername) {
        Usuario autor = usuarioRepository.findByUsernameIgnoreCase(autorUsername)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String codigo = NumeracionUtil.formatearFormulario(encuestaRepository.count() + 1);

        Encuesta encuesta = Encuesta.builder()
                .codigo(codigo)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .estado(EstadoEncuesta.BORRADOR)
                .publico(request.publico())
                .requiereAutenticacion(request.requiereAutenticacion())
                .respuestaUnica(request.respuestaUnica())
                .respuestasAnonimas(request.respuestasAnonimas())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .autor(autor)
                .build();

        encuesta = encuestaRepository.save(encuesta);
        encuesta.setCodigoQr(qrCodeService.generarQrFormulario(encuesta.getCodigo()));

        if (request.preguntas() != null) {
            int orden = 1;
            for (PreguntaEncuestaRequest pReq : request.preguntas()) {
                PreguntaEncuesta pregunta = PreguntaEncuesta.builder()
                        .encuesta(encuesta)
                        .texto(pReq.texto())
                        .tipo(pReq.tipo())
                        .obligatoria(pReq.obligatoria())
                        .orden(pReq.orden() != null ? pReq.orden() : orden++)
                        .opciones(pReq.opciones() != null ? String.join("|", pReq.opciones()) : null)
                        .build();
                encuesta.getPreguntas().add(pregunta);
            }
        }

        encuesta = encuestaRepository.save(encuesta);
        auditoriaService.registrar("CREAR_ENCUESTA", "ENCUESTAS", encuesta.getCodigo(), null);
        return EncuestaResponse.fromEntity(encuesta);
    }

    @Transactional
    public EncuestaResponse activar(Long id) {
        Encuesta encuesta = obtenerEntidad(id);
        encuesta.setEstado(EstadoEncuesta.ACTIVA);
        encuesta = encuestaRepository.save(encuesta);
        auditoriaService.registrar("ACTIVAR_ENCUESTA", "ENCUESTAS", encuesta.getCodigo(), null);
        return EncuestaResponse.fromEntity(encuesta);
    }

    @Transactional
    public EncuestaResponse desactivar(Long id) {
        Encuesta encuesta = obtenerEntidad(id);
        encuesta.setEstado(EstadoEncuesta.FINALIZADA);
        encuesta = encuestaRepository.save(encuesta);
        auditoriaService.registrar("DESACTIVAR_ENCUESTA", "ENCUESTAS", encuesta.getCodigo(), null);
        return EncuestaResponse.fromEntity(encuesta);
    }

    /** Los formularios con respuestas registradas no se eliminan; solo se archivan (12.13). */
    @Transactional
    public void archivar(Long id) {
        Encuesta encuesta = obtenerEntidad(id);
        encuesta.setEstado(EstadoEncuesta.ARCHIVADA);
        encuestaRepository.save(encuesta);
        auditoriaService.registrar("ARCHIVAR_ENCUESTA", "ENCUESTAS", encuesta.getCodigo(), null);
    }

    @Transactional
    public void responder(Long encuestaId, ResponderEncuestaRequest request, Usuario usuarioAutenticado, String ip) {
        Encuesta encuesta = obtenerEntidad(encuestaId);

        if (encuesta.getEstado() != EstadoEncuesta.ACTIVA) {
            throw new ReglaNegocioException("Este formulario no esta activo y no acepta nuevas respuestas.");
        }
        if (encuesta.isRequiereAutenticacion() && usuarioAutenticado == null) {
            throw new ReglaNegocioException("Este formulario requiere autenticacion para responder.");
        }
        if (encuesta.isRespuestaUnica() && usuarioAutenticado != null
                && respuestaEncuestaRepository.existsByEncuestaIdAndUsuarioId(encuestaId, usuarioAutenticado.getId())) {
            throw new ReglaNegocioException("Ya ha respondido este formulario. Solo se permite una respuesta por participante.");
        }

        boolean esAnonima = encuesta.isRespuestasAnonimas();

        // Con respuestas anonimas activadas nunca se guarda ni se exige identidad, ni de cuenta ni de nombre.
        Usuario usuarioAGuardar = esAnonima ? null : usuarioAutenticado;
        String nombreAGuardar = null;

        if (!esAnonima && usuarioAutenticado == null) {
            // Formulario NO anonimo y quien responde no tiene sesion: el nombre pasa a ser obligatorio.
            if (request.nombre() == null || request.nombre().isBlank()) {
                throw new ReglaNegocioException("Este formulario no es anonimo, por lo tanto el nombre es obligatorio para responder.");
            }
            nombreAGuardar = request.nombre().trim();
        }

        RespuestaEncuesta respuestaEncuesta = RespuestaEncuesta.builder()
                .encuesta(encuesta)
                .usuario(usuarioAGuardar)
                .nombreRespondiente(nombreAGuardar)
                .fecha(LocalDateTime.now())
                .ip(ip)
                .build();
        respuestaEncuesta = respuestaEncuestaRepository.save(respuestaEncuesta);

        Map<Long, PreguntaEncuesta> preguntasPorId = encuesta.getPreguntas().stream()
                .collect(Collectors.toMap(PreguntaEncuesta::getId, p -> p));

        for (var item : request.respuestas()) {
            PreguntaEncuesta pregunta = preguntasPorId.get(item.preguntaId());
            if (pregunta == null) {
                throw new RecursoNoEncontradoException("La pregunta " + item.preguntaId() + " no pertenece a este formulario.");
            }
            RespuestaPregunta rp = RespuestaPregunta.builder()
                    .respuestaEncuesta(respuestaEncuesta)
                    .pregunta(pregunta)
                    .valor(item.valor())
                    .build();
            respuestaPreguntaRepository.save(rp);
        }

        auditoriaService.registrar("RESPONDER_ENCUESTA", "ENCUESTAS", encuesta.getCodigo(), null);
    }

    /** Devuelve todas las respuestas registradas para un formulario, con el nombre de quien respondio (12.13). */
    public List<RespuestaEncuestaResponse> listarRespuestas(Long encuestaId) {
        obtenerEntidad(encuestaId); // valida que el formulario exista
        return respuestaEncuestaRepository.findByEncuestaId(encuestaId).stream()
                .map(RespuestaEncuestaResponse::fromEntity)
                .toList();
    }

    public EncuestaEstadisticasResponse estadisticas(Long encuestaId) {
        Encuesta encuesta = obtenerEntidad(encuestaId);
        long totalRespuestas = respuestaEncuestaRepository.countByEncuestaId(encuestaId);

        Map<String, Long> resumen = new HashMap<>();
        for (PreguntaEncuesta pregunta : encuesta.getPreguntas()) {
            long conteo = pregunta.getRespuestas() != null ? pregunta.getRespuestas().size() : 0;
            resumen.put(pregunta.getTexto(), conteo);
        }

        return EncuestaEstadisticasResponse.builder().totalRespuestas(totalRespuestas).resumenPorPregunta(resumen).build();
    }

    public EncuestaResponse obtener(Long id) {
        return EncuestaResponse.fromEntity(obtenerEntidad(id));
    }

    /** Resuelve una encuesta a partir del codigo impreso/codificado en su QR (12.14). */
    public EncuestaResponse obtenerPorCodigo(String codigo) {
        Encuesta encuesta = encuestaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro un formulario con el codigo " + codigo));
        return EncuestaResponse.fromEntity(encuesta);
    }

    public Encuesta obtenerEntidad(Long id) {
        return encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada con id " + id));
    }

    public List<EncuestaResponse> listarActivas() {
        return encuestaRepository.findByEstado(EstadoEncuesta.ACTIVA).stream().map(EncuestaResponse::fromEntity).toList();
    }

    public List<EncuestaResponse> listarTodas() {
        return encuestaRepository.findAll().stream().map(EncuestaResponse::fromEntity).toList();
    }
}
