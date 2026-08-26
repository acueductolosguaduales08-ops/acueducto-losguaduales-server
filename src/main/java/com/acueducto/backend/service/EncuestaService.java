package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.EncuestaRequest;
import com.acueducto.backend.dto.request.PreguntaEncuestaRequest;
import com.acueducto.backend.dto.request.ResponderEncuestaRequest;
import com.acueducto.backend.dto.response.EncuestaEstadisticasResponse;
import com.acueducto.backend.dto.response.EncuestaResponse;
import com.acueducto.backend.dto.response.RespuestaEncuestaResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoEncuesta;
import com.acueducto.backend.entity.enums.TipoPregunta;
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

        LocalDateTime ahora = LocalDateTime.now();
        if (request.fechaInicio() != null && !request.fechaInicio().isAfter(ahora)) {
            throw new ReglaNegocioException("La fecha de inicio programada debe ser mayor a la fecha y hora actual.");
        }
        if (request.fechaFin() != null) {
            if (!request.fechaFin().isAfter(ahora)) {
                throw new ReglaNegocioException("La fecha de fin programada debe ser mayor a la fecha y hora actual.");
            }
            if (request.fechaInicio() != null && !request.fechaFin().isAfter(request.fechaInicio())) {
                throw new ReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio.");
            }
        }

        String codigo = NumeracionUtil.formatearFormulario(encuestaRepository.findMaxNumeroCodigo().orElse(0L) + 1);

        // "publico" controla el arranque: si no tiene programacion (fechaInicio), un formulario
        // publico se activa de inmediato al crearse en vez de quedar en borrador. Si SI tiene
        // fechaInicio, el arranque lo maneja la programacion (sincronizarEstado), no esta bandera.
        EstadoEncuesta estadoInicial = (request.publico() && request.fechaInicio() == null)
                ? EstadoEncuesta.ACTIVA
                : EstadoEncuesta.BORRADOR;

        Encuesta encuesta = Encuesta.builder()
                .codigo(codigo)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .estado(estadoInicial)
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

        // Cuantas respuestas llegaron por pregunta: todo lo que no sea OPCION_MULTIPLE admite
        // como mucho una (para OPCION_MULTIPLE, el frontend puede mandar varios items con el
        // mismo preguntaId, uno por cada opcion elegida).
        Map<Long, Long> conteoPorPregunta = request.respuestas().stream()
                .collect(Collectors.groupingBy(ResponderEncuestaRequest.RespuestaPreguntaItem::preguntaId, Collectors.counting()));

        for (var item : request.respuestas()) {
            PreguntaEncuesta pregunta = preguntasPorId.get(item.preguntaId());
            if (pregunta == null) {
                throw new RecursoNoEncontradoException("La pregunta " + item.preguntaId() + " no pertenece a este formulario.");
            }
            if (pregunta.getTipo() != TipoPregunta.OPCION_MULTIPLE && conteoPorPregunta.get(item.preguntaId()) > 1) {
                throw new ReglaNegocioException(
                        "La pregunta '" + pregunta.getTexto() + "' no es de opcion multiple: solo admite una respuesta.");
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

    public EncuestaResponse obtener(Long id, Usuario usuarioAutenticado) {
        Encuesta encuesta = obtenerEntidad(id);
        verificarPuedeVer(encuesta, usuarioAutenticado);
        return EncuestaResponse.fromEntity(encuesta);
    }

    /** Resuelve una encuesta a partir del codigo impreso/codificado en su QR (12.14). */
    public EncuestaResponse obtenerPorCodigo(String codigo, Usuario usuarioAutenticado) {
        Encuesta encuesta = encuestaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro un formulario con el codigo " + codigo));
        encuesta = sincronizarEstado(encuesta);
        verificarPuedeVer(encuesta, usuarioAutenticado);
        return EncuestaResponse.fromEntity(encuesta);
    }

    /** "no la puede ver sin inicio de sesion" cuando requiereAutenticacion esta activo (10 / 12.13). */
    private void verificarPuedeVer(Encuesta encuesta, Usuario usuarioAutenticado) {
        if (encuesta.isRequiereAutenticacion() && usuarioAutenticado == null) {
            throw new com.acueducto.backend.exception.AccesoDenegadoModuloException(
                    "Este formulario requiere iniciar sesion para verlo.");
        }
    }

    @Transactional
    public Encuesta obtenerEntidad(Long id) {
        Encuesta encuesta = encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada con id " + id));
        return sincronizarEstado(encuesta);
    }

    @Transactional
    public List<EncuestaResponse> listarActivas() {
        sincronizarTodas();
        return encuestaRepository.findByEstado(EstadoEncuesta.ACTIVA).stream().map(EncuestaResponse::fromEntity).toList();
    }

    @Transactional
    public List<EncuestaResponse> listarTodas() {
        sincronizarTodas();
        return encuestaRepository.findAll().stream().map(EncuestaResponse::fromEntity).toList();
    }

    /**
     * Corrige el estado de UNA encuesta segun su programacion (fechaInicio/fechaFin), comparando
     * contra el momento en que se llama este metodo. Se usa "al leer" en vez de con una tarea de
     * fondo porque el servicio esta en Render, cuyo plan gratuito se apaga por inactividad; una
     * tarea programada (@Scheduled) simplemente no correria mientras el servicio esta dormido, asi
     * que la unica forma confiable de que la programacion "funcione siempre" es recalcularla en el
     * momento en que alguien efectivamente consulta o usa la encuesta (ver tambien
     * TareasProgramadasService.sincronizarEncuestasProgramadas, que hace un barrido best-effort
     * mientras el servicio esta despierto).
     *
     * Reglas: "si en la hora programada ya esta abierto no cambia" se logra porque BORRADOR->ACTIVA
     * solo dispara si el estado actual es BORRADOR (si ya esta ACTIVA, no hace nada). Nunca reabre
     * una encuesta FINALIZADA o ARCHIVADA (esas son terminales: solo se cambian a mano). Un
     * administrador que activa/desactiva manualmente sigue pudiendo hacerlo en cualquier momento;
     * esta sincronizacion no lo bloquea, solo actua cuando el estado quedo "atrasado".
     */
    Encuesta sincronizarEstado(Encuesta encuesta) {
        LocalDateTime ahora = LocalDateTime.now();
        boolean cambio = false;

        if (encuesta.getEstado() == EstadoEncuesta.BORRADOR
                && encuesta.getFechaInicio() != null
                && !ahora.isBefore(encuesta.getFechaInicio())) {
            encuesta.setEstado(EstadoEncuesta.ACTIVA);
            cambio = true;
        }

        if (encuesta.getEstado() == EstadoEncuesta.ACTIVA
                && encuesta.getFechaFin() != null
                && !ahora.isBefore(encuesta.getFechaFin())) {
            encuesta.setEstado(EstadoEncuesta.FINALIZADA);
            cambio = true;
        }

        return cambio ? encuestaRepository.save(encuesta) : encuesta;
    }

    /** Pasada completa de sincronizarEstado sobre todo lo que todavia puede cambiar de estado solo. */
    void sincronizarTodas() {
        encuestaRepository.findByEstadoIn(List.of(EstadoEncuesta.BORRADOR, EstadoEncuesta.ACTIVA))
                .forEach(this::sincronizarEstado);
    }
}
