package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.ConversacionResponse;
import com.acueducto.backend.dto.response.ParticipanteResponse;
import com.acueducto.backend.dto.response.MensajeResponse;
import com.acueducto.backend.dto.response.SolicitudEliminacionResponse;
import com.acueducto.backend.entity.Conversacion;
import com.acueducto.backend.entity.Mensaje;
import com.acueducto.backend.entity.SolicitudEliminacion;
import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.EstadoSolicitudEliminacion;
import com.acueducto.backend.entity.enums.Rol;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.ConversacionRepository;
import com.acueducto.backend.repository.MensajeRepository;
import com.acueducto.backend.repository.SolicitudEliminacionRepository;
import com.acueducto.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sistema de chat interno: mensajeria de texto/emojis entre Asociado, Tesorero y
 * Administrador. Toda operacion valida al usuario autenticado (JWT), su participacion
 * en la conversacion y la propiedad de los mensajes. Los mensajes se retienen en
 * PostgreSQL 8 dias y luego se eliminan automaticamente (el historial completo vive
 * en IndexedDB del cliente).
 *
 * La eliminacion de mensajes NUNCA es directa: flujo Solicitud -> Confirmacion del
 * otro participante -> Eliminacion definitiva. La eliminacion de una conversacion
 * completa es independiente y solo la permiten ADMINISTRADOR/TESORERO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENIDO = 2000;
    private static final int DIAS_RETENCION = 8;
    /** Caracteres de control que no se admiten en el contenido (se permiten texto y emojis). */
    private static final Pattern CARACTERES_NO_PERMITIDOS =
            Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]");

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final SolicitudEliminacionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;

    // ==================== CONVERSACIONES ====================

    /**
     * Devuelve los contactos disponibles para iniciar una conversacion.
     * Admin/Tesorero ven los asociados; el asociado ve administradores y tesoreros.
     */
    public List<ParticipanteResponse> obtenerContactos(Long usuarioActualId) {
        Usuario actual = obtenerUsuario(usuarioActualId);
        List<Rol> rolesContactos;
        if (actual.getRol() == Rol.ASOCIADO) {
            rolesContactos = List.of(Rol.ADMINISTRADOR, Rol.TESORERO);
        } else {
            rolesContactos = List.of(Rol.ASOCIADO);
        }
        return usuarioRepository.findByRolInAndActivoTrue(rolesContactos).stream()
                .filter(u -> !u.getId().equals(usuarioActualId))
                .map(ParticipanteResponse::fromUsuario)
                .toList();
    }

    /**
     * Crea una conversacion con el destinatario, o devuelve la existente si la pareja
     * ya tiene una. Nunca se crean conversaciones duplicadas entre los mismos usuarios.
     */
    @Transactional
    public ConversacionResponse crearConversacion(Long usuarioActualId, Long destinatarioId) {
        Usuario actual = obtenerUsuario(usuarioActualId);
        Usuario destinatario = obtenerUsuario(destinatarioId);

        if (actual.getId().equals(destinatario.getId())) {
            throw new ReglaNegocioException("No puede crear una conversacion consigo mismo.");
        }
        if (!actual.isActivo() || !destinatario.isActivo()) {
            throw new ReglaNegocioException("Ambos usuarios deben estar activos para conversar.");
        }
        if (actual.getRol() == Rol.ASOCIADO && destinatario.getRol() == Rol.ASOCIADO) {
            throw new ReglaNegocioException("Un asociado solo puede comunicarse con un administrador o tesorero.");
        }

        Long menorId = Math.min(actual.getId(), destinatario.getId());
        Long mayorId = Math.max(actual.getId(), destinatario.getId());

        Conversacion existente = conversacionRepository.findByUsuario1IdAndUsuario2Id(menorId, mayorId).orElse(null);
        if (existente != null) {
            return toResponse(existente, usuarioActualId);
        }

        Conversacion conversacion = Conversacion.builder()
                .usuario1(obtenerUsuario(menorId))
                .usuario2(obtenerUsuario(mayorId))
                .activa(true)
                .build();
        conversacion = conversacionRepository.save(conversacion);
        auditoriaService.registrar("CREAR_CONVERSACION", "CHAT", "Conversacion #" + conversacion.getId(), null);
        return toResponse(conversacion, usuarioActualId);
    }

    public List<ConversacionResponse> listarConversaciones(Long usuarioActualId) {
        List<Conversacion> conversaciones = conversacionRepository.findByUsuario1IdOrUsuario2Id(usuarioActualId, usuarioActualId);
        return conversaciones.stream()
                .map(c -> toResponse(c, usuarioActualId))
                .sorted(Comparator.comparing(ConversacionResponse::getFechaUltimoMensaje,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // ==================== MENSAJES ====================

    @Transactional(readOnly = true)
    public List<MensajeResponse> listarMensajes(Long usuarioActualId, Long conversacionId, Long desde, LocalDateTime desdeFecha) {
        obtenerConversacionParticipante(conversacionId, usuarioActualId);
        List<Mensaje> mensajes;
        if (desde != null) {
            mensajes = mensajeRepository.findByConversacionIdAndIdGreaterThanOrderByIdAsc(conversacionId, desde);
        } else if (desdeFecha != null) {
            mensajes = mensajeRepository.findByConversacionIdAndFechaCreacionGreaterThanOrderByIdAsc(conversacionId, desdeFecha);
        } else {
            mensajes = mensajeRepository.findByConversacionIdOrderByIdAsc(conversacionId);
        }
        return mensajes.stream().map(MensajeResponse::fromEntity).toList();
    }

    @Transactional
    public MensajeResponse enviarMensaje(Long usuarioActualId, Long conversacionId, String contenido) {
        Conversacion conversacion = obtenerConversacionParticipante(conversacionId, usuarioActualId);
        String limpio = validarContenido(contenido);
        Usuario remitente = obtenerUsuario(usuarioActualId);

        Mensaje mensaje = Mensaje.builder()
                .conversacion(conversacion)
                .remitente(remitente)
                .contenido(limpio)
                .editado(false)
                .leido(false)
                .build();
        mensaje = mensajeRepository.save(mensaje);
        return MensajeResponse.fromEntity(mensaje);
    }

    /** Solo el autor del mensaje puede editarlo. Se conservan fecha original y autor. */
    @Transactional
    public MensajeResponse editarMensaje(Long usuarioActualId, Long mensajeId, String contenido) {
        Mensaje mensaje = obtenerMensaje(mensajeId);
        if (!mensaje.getRemitente().getId().equals(usuarioActualId)) {
            throw new ReglaNegocioException("Solo el autor del mensaje puede editarlo.");
        }
        String limpio = validarContenido(contenido);
        mensaje.setContenido(limpio);
        mensaje.setEditado(true);
        mensaje = mensajeRepository.save(mensaje);
        auditoriaService.registrar("EDITAR_MENSAJE_CHAT", "CHAT", "Mensaje #" + mensaje.getId(), null);
        return MensajeResponse.fromEntity(mensaje);
    }

    /** Marca como leidos los mensajes recibidos por el usuario autenticado en la conversacion. */
    @Transactional
    public void marcarLeidos(Long usuarioActualId, Long conversacionId) {
        obtenerConversacionParticipante(conversacionId, usuarioActualId);
        mensajeRepository.marcarLeidosPorConversacion(conversacionId, usuarioActualId, LocalDateTime.now());
    }

    // ==================== SOLICITUDES DE ELIMINACION ====================

    /** El autor solicita eliminar su propio mensaje. El mensaje NO se elimina todavia. */
    @Transactional
    public SolicitudEliminacionResponse solicitarEliminacion(Long usuarioActualId, Long mensajeId) {
        Mensaje mensaje = obtenerMensaje(mensajeId);
        if (!mensaje.getRemitente().getId().equals(usuarioActualId)) {
            throw new ReglaNegocioException("Solo el autor del mensaje puede solicitar su eliminacion.");
        }
        if (solicitudRepository.existsByMensajeIdAndEstado(mensajeId, EstadoSolicitudEliminacion.PENDIENTE)) {
            throw new ReglaNegocioException("Ya existe una solicitud de eliminacion pendiente para este mensaje.");
        }
        Usuario confirmador = otroParticipante(mensaje.getConversacion(), usuarioActualId);

        SolicitudEliminacion solicitud = SolicitudEliminacion.builder()
                .mensaje(mensaje)
                .solicitante(obtenerUsuario(usuarioActualId))
                .confirmador(confirmador)
                .estado(EstadoSolicitudEliminacion.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();
        solicitud = solicitudRepository.save(solicitud);
        auditoriaService.registrar("SOLICITAR_ELIMINACION_MENSAJE", "CHAT", "Mensaje #" + mensajeId, null);
        return SolicitudEliminacionResponse.fromEntity(solicitud, usuarioActualId);
    }

    /** Solicitudes pendientes del usuario autenticado: las que debe confirmar y las que envio. */
    @Transactional(readOnly = true)
    public List<SolicitudEliminacionResponse> listarSolicitudes(Long usuarioActualId) {
        List<SolicitudEliminacion> recibidas = solicitudRepository.findByConfirmadorIdAndEstado(
                usuarioActualId, EstadoSolicitudEliminacion.PENDIENTE);
        List<SolicitudEliminacion> enviadas = solicitudRepository.findBySolicitanteIdAndEstado(
                usuarioActualId, EstadoSolicitudEliminacion.PENDIENTE);
        return Stream.concat(recibidas.stream(), enviadas.stream())
                .sorted(Comparator.comparing(SolicitudEliminacion::getFechaSolicitud).reversed())
                .map(s -> SolicitudEliminacionResponse.fromEntity(s, usuarioActualId))
                .toList();
    }

    /** Solo el confirmador puede aceptar. La aceptacion elimina definitivamente el mensaje en una transaccion. */
    @Transactional
    public SolicitudEliminacionResponse aceptarSolicitud(Long usuarioActualId, Long solicitudId) {
        SolicitudEliminacion solicitud = obtenerSolicitud(solicitudId);
        if (!solicitud.getConfirmador().getId().equals(usuarioActualId)) {
            throw new ReglaNegocioException("Solo el usuario autorizado a confirmar puede aceptar la solicitud.");
        }
        if (solicitud.getEstado() != EstadoSolicitudEliminacion.PENDIENTE) {
            throw new ReglaNegocioException("La solicitud ya fue resuelta.");
        }

        Long mensajeId = solicitud.getMensaje().getId();
        Usuario solicitante = solicitud.getSolicitante();

        solicitud.setEstado(EstadoSolicitudEliminacion.ACEPTADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitudRepository.saveAndFlush(solicitud);

        SolicitudEliminacionResponse respuesta = SolicitudEliminacionResponse.fromEntity(solicitud, usuarioActualId);

        // Eliminar el mensaje y cualquier dato relacionado (incluida la propia solicitud).
        solicitudRepository.deleteByMensajeId(mensajeId);
        mensajeRepository.deleteById(mensajeId);

        auditoriaService.registrar("ACEPTAR_ELIMINACION_MENSAJE", "CHAT", "Mensaje #" + mensajeId, null);
        notificacionService.notificarEventoChat(solicitante, "Mensaje eliminado",
                "Tu solicitud de eliminacion fue aceptada",
                "El mensaje que solicitaste eliminar fue eliminado definitivamente.");
        respuesta.setMensajeEliminado(true);
        return respuesta;
    }

    /** Solo el confirmador puede rechazar. El mensaje se conserva y vuelve a mostrarse. */
    @Transactional
    public SolicitudEliminacionResponse rechazarSolicitud(Long usuarioActualId, Long solicitudId) {
        SolicitudEliminacion solicitud = obtenerSolicitud(solicitudId);
        if (!solicitud.getConfirmador().getId().equals(usuarioActualId)) {
            throw new ReglaNegocioException("Solo el usuario autorizado a confirmar puede rechazar la solicitud.");
        }
        if (solicitud.getEstado() != EstadoSolicitudEliminacion.PENDIENTE) {
            throw new ReglaNegocioException("La solicitud ya fue resuelta.");
        }

        solicitud.setEstado(EstadoSolicitudEliminacion.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud = solicitudRepository.save(solicitud);

        auditoriaService.registrar("RECHAZAR_ELIMINACION_MENSAJE", "CHAT", "Mensaje #" + solicitud.getMensaje().getId(), null);
        notificacionService.notificarEventoChat(solicitud.getSolicitante(), "Solicitud de eliminacion rechazada",
                "El otro participante rechazo tu solicitud",
                "Tu solicitud para eliminar un mensaje fue rechazada. El mensaje sigue visible.");
        return SolicitudEliminacionResponse.fromEntity(solicitud, usuarioActualId);
    }

    /** Solo el solicitante puede cancelar su propia solicitud pendiente. */
    @Transactional
    public SolicitudEliminacionResponse cancelarSolicitud(Long usuarioActualId, Long solicitudId) {
        SolicitudEliminacion solicitud = obtenerSolicitud(solicitudId);
        if (!solicitud.getSolicitante().getId().equals(usuarioActualId)) {
            throw new ReglaNegocioException("Solo el usuario que envio la solicitud puede cancelarla.");
        }
        if (solicitud.getEstado() != EstadoSolicitudEliminacion.PENDIENTE) {
            throw new ReglaNegocioException("La solicitud ya fue resuelta.");
        }

        solicitud.setEstado(EstadoSolicitudEliminacion.CANCELADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud = solicitudRepository.save(solicitud);

        auditoriaService.registrar("CANCELAR_SOLICITUD_ELIMINACION", "CHAT", "Mensaje #" + solicitud.getMensaje().getId(), null);
        return SolicitudEliminacionResponse.fromEntity(solicitud, usuarioActualId);
    }

    // ==================== ELIMINACION COMPLETA DE CONVERSACION ====================

    /**
     * Elimina una conversacion completa de forma definitiva (solo ADMINISTRADOR/TESORERO,
     * validado en el controlador). Se borran solicitudes, mensajes y la conversacion en
     * una sola transaccion para no dejar datos huerfanos.
     */
    @Transactional
    public void eliminarConversacion(Long usuarioActualId, Long conversacionId) {
        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversacion no encontrada con id " + conversacionId));
        boolean participa = conversacion.getUsuario1().getId().equals(usuarioActualId)
                || conversacion.getUsuario2().getId().equals(usuarioActualId);
        if (!participa) {
            throw new ReglaNegocioException("No tiene acceso a esta conversacion.");
        }

        solicitudRepository.deleteByMensaje_ConversacionId(conversacionId);
        mensajeRepository.deleteByConversacionId(conversacionId);
        conversacionRepository.delete(conversacion);
        auditoriaService.registrar("ELIMINAR_CONVERSACION", "CHAT", "Conversacion #" + conversacionId, null);
    }

    // ==================== RETENCION DE 8 DIAS ====================

    /**
     * Elimina directamente en PostgreSQL los mensajes con mas de 8 dias y sus solicitudes
     * de eliminacion asociadas. Se ejecuta cada dia via @Scheduled (TareasProgramadasService).
     */
    @Transactional
    public int eliminarMensajesAntiguos() {
        LocalDateTime corte = LocalDateTime.now().minusDays(DIAS_RETENCION);
        List<Long> ids = mensajeRepository.findIdsByFechaCreacionBefore(corte);
        if (ids.isEmpty()) {
            return 0;
        }
        solicitudRepository.deleteByMensajeIdIn(ids);
        return mensajeRepository.deleteByIdIn(ids);
    }

    // ==================== HELPERS ====================

    private ConversacionResponse toResponse(Conversacion conversacion, Long usuarioActualId) {
        Usuario otro = otroParticipante(conversacion, usuarioActualId);
        Mensaje ultimo = mensajeRepository.findTopByConversacionIdOrderByIdDesc(conversacion.getId()).orElse(null);
        long noLeidos = mensajeRepository.countByConversacionIdAndRemitenteIdNotAndLeidoFalse(conversacion.getId(), usuarioActualId);
        boolean pendienteEnviada = solicitudRepository.existsBySolicitanteIdAndEstadoAndMensaje_ConversacionId(
                usuarioActualId, EstadoSolicitudEliminacion.PENDIENTE, conversacion.getId());
        boolean pendienteRecibida = solicitudRepository.existsByConfirmadorIdAndEstadoAndMensaje_ConversacionId(
                usuarioActualId, EstadoSolicitudEliminacion.PENDIENTE, conversacion.getId());

        return ConversacionResponse.builder()
                .id(conversacion.getId())
                .participante(ParticipanteResponse.fromUsuario(otro))
                .ultimoMensaje(ultimo != null ? ultimo.getContenido() : null)
                .fechaUltimoMensaje(ultimo != null ? ultimo.getFechaCreacion() : null)
                .noLeidos(noLeidos)
                .solicitudPendienteEnviada(pendienteEnviada)
                .solicitudPendienteRecibida(pendienteRecibida)
                .build();
    }

    private Usuario otroParticipante(Conversacion conversacion, Long usuarioActualId) {
        return conversacion.getUsuario1().getId().equals(usuarioActualId)
                ? conversacion.getUsuario2()
                : conversacion.getUsuario1();
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + id));
    }

    private Conversacion obtenerConversacionParticipante(Long conversacionId, Long usuarioId) {
        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conversacion no encontrada con id " + conversacionId));
        boolean participa = conversacion.getUsuario1().getId().equals(usuarioId)
                || conversacion.getUsuario2().getId().equals(usuarioId);
        if (!participa) {
            throw new ReglaNegocioException("No tiene acceso a esta conversacion.");
        }
        return conversacion;
    }

    private Mensaje obtenerMensaje(Long mensajeId) {
        return mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado con id " + mensajeId));
    }

    private SolicitudEliminacion obtenerSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Solicitud de eliminacion no encontrada con id " + solicitudId));
    }

    /** Sanitiza el contenido: elimina caracteres de control y valida longitud. Solo texto y emojis. */
    private String validarContenido(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new ReglaNegocioException("El mensaje no puede estar vacio.");
        }
        String limpio = CARACTERES_NO_PERMITIDOS.matcher(contenido).replaceAll("").trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("El mensaje no puede estar vacio.");
        }
        if (limpio.length() > MAX_CONTENIDO) {
            throw new ReglaNegocioException("El mensaje no puede superar los " + MAX_CONTENIDO + " caracteres.");
        }
        return limpio;
    }
}