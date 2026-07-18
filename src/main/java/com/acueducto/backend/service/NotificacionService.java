package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.NotificacionRequest;
import com.acueducto.backend.dto.response.NotificacionResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoNotificacion;
import com.acueducto.backend.entity.enums.PrioridadNotificacion;
import com.acueducto.backend.entity.enums.TipoNotificacion;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.NotificacionLecturaRepository;
import com.acueducto.backend.repository.NotificacionRepository;
import com.acueducto.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Modulo de Notificaciones (Modulo 13). Genera avisos automaticos ante eventos de otros
 * modulos (facturacion, pagos, formularios) y permite notificaciones manuales del Administrador/Tesorero.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionLecturaRepository notificacionLecturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public NotificacionResponse crear(NotificacionRequest request, String autorUsername) {
        Usuario autor = usuarioRepository.findByUsernameIgnoreCase(autorUsername)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Usuario destinatario = null;
        if (request.destinatarioId() != null) {
            destinatario = usuarioRepository.findById(request.destinatarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Destinatario no encontrado"));
        }

        Notificacion notificacion = Notificacion.builder()
                .titulo(request.titulo())
                .descripcionCorta(request.descripcionCorta())
                .contenidoCompleto(request.contenidoCompleto())
                .tipo(request.tipo())
                .prioridad(request.prioridad() != null ? request.prioridad() : PrioridadNotificacion.NORMAL)
                .autor(autor)
                .destinatario(destinatario)
                .fechaPublicacion(request.fechaPublicacion() != null ? request.fechaPublicacion() : LocalDateTime.now())
                .fechaVencimiento(request.fechaVencimiento())
                .enlaceUrl(request.enlaceUrl())
                .estado(request.fechaPublicacion() != null && request.fechaPublicacion().isAfter(LocalDateTime.now())
                        ? EstadoNotificacion.PROGRAMADA : EstadoNotificacion.ACTIVA)
                .build();

        notificacion = notificacionRepository.save(notificacion);
        auditoriaService.registrar("CREAR_NOTIFICACION", "NOTIFICACIONES", notificacion.getTitulo(), null);
        return NotificacionResponse.fromEntity(notificacion, false);
    }

    // ---- Notificaciones automaticas generadas por otros modulos (13.10) ----

    public void notificarFacturaGenerada(Factura factura) {
        Usuario destinatario = usuarioRepository.findByAsociadoId(factura.getAsociado().getId()).orElse(null);
        crearNotificacionAutomatica(
                "Nueva factura disponible",
                "Se genero la factura " + factura.getNumeroFactura(),
                "Su factura " + factura.getNumeroFactura() + " por valor de $" + factura.getTotal()
                        + " ya esta disponible. Fecha limite de pago: " + factura.getFechaLimitePago() + ".",
                destinatario, "/factura/" + factura.getNumeroFactura());
    }

    public void notificarPagoRegistrado(Recibo recibo) {
        Usuario destinatario = usuarioRepository.findByAsociadoId(recibo.getAsociado().getId()).orElse(null);
        crearNotificacionAutomatica(
                "Pago registrado",
                "Se registro un pago sobre la factura " + recibo.getFactura().getNumeroFactura(),
                "Su pago fue registrado correctamente. Recibo " + recibo.getNumeroRecibo()
                        + " por valor de $" + recibo.getValor() + ".",
                destinatario, "/recibo/" + recibo.getNumeroRecibo());
    }

    private void crearNotificacionAutomatica(String titulo, String descripcionCorta, String contenido,
                                              Usuario destinatario, String enlace) {
        Usuario autorSistema = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == com.acueducto.backend.entity.enums.Rol.ADMINISTRADOR)
                .findFirst().orElse(null);
        if (autorSistema == null) return; // aun no existe un administrador (arranque inicial)

        Notificacion notificacion = Notificacion.builder()
                .titulo(titulo)
                .descripcionCorta(descripcionCorta)
                .contenidoCompleto(contenido)
                .tipo(TipoNotificacion.ASOCIADO)
                .prioridad(PrioridadNotificacion.NORMAL)
                .estado(EstadoNotificacion.ACTIVA)
                .autor(autorSistema)
                .destinatario(destinatario)
                .fechaPublicacion(LocalDateTime.now())
                .enlaceUrl(enlace)
                .build();
        notificacionRepository.save(notificacion);
    }

    /**
     * Notifica a todos los Administradores y Tesoreros activos sobre un nuevo reporte
     * ciudadano (fuga, queja o reclamo). Se apoya en el mismo modulo de notificaciones
     * para no interferir con los demas modulos del sistema.
     */
    public void notificarNuevoReporteCiudadano(com.acueducto.backend.entity.ReporteCiudadano reporte) {
        Usuario autorSistema = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == com.acueducto.backend.entity.enums.Rol.ADMINISTRADOR)
                .findFirst().orElse(null);
        if (autorSistema == null) return; // aun no existe un administrador (arranque inicial)

        java.util.List<Usuario> destinatarios = usuarioRepository.findByRolInAndActivoTrue(java.util.List.of(
                com.acueducto.backend.entity.enums.Rol.ADMINISTRADOR,
                com.acueducto.backend.entity.enums.Rol.TESORERO));

        String contenido = "Se envio el " + reporte.getFechaCreacion()
                + ", reportado por " + reporte.getNombre()
                + ". Se eliminara automaticamente el " + reporte.getFechaEliminacion() + ".";

        for (Usuario destinatario : destinatarios) {
            Notificacion notificacion = Notificacion.builder()
                    .titulo("Nuevo reporte de fuga/queja recibido")
                    .descripcionCorta("Reporte enviado por " + reporte.getNombre())
                    .contenidoCompleto(contenido)
                    .tipo(TipoNotificacion.ADMINISTRATIVA)
                    .prioridad(PrioridadNotificacion.ALTA)
                    .estado(EstadoNotificacion.ACTIVA)
                    .autor(autorSistema)
                    .destinatario(destinatario)
                    .fechaPublicacion(LocalDateTime.now())
                    .build();
            notificacionRepository.save(notificacion);
        }
    }

    @Transactional
    public void marcarLeida(Long notificacionId, Long usuarioId) {
        var existente = notificacionLecturaRepository.findByNotificacionIdAndUsuarioId(notificacionId, usuarioId);
        if (existente.isPresent()) {
            existente.get().setLeida(true);
            existente.get().setFechaLectura(LocalDateTime.now());
            notificacionLecturaRepository.save(existente.get());
            return;
        }
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion no encontrada"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        NotificacionLectura lectura = NotificacionLectura.builder()
                .notificacion(notificacion).usuario(usuario).leida(true).fechaLectura(LocalDateTime.now())
                .build();
        notificacionLecturaRepository.save(lectura);
    }

    public Page<NotificacionResponse> listarPublicas(Pageable pageable) {
        return notificacionRepository.findByTipo(TipoNotificacion.PUBLICA, pageable)
                .map(n -> NotificacionResponse.fromEntity(n, false));
    }

    public Page<NotificacionResponse> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return notificacionRepository.findByDestinatarioId(usuarioId, pageable)
                .map(n -> {
                    boolean leida = notificacionLecturaRepository.findByNotificacionIdAndUsuarioId(n.getId(), usuarioId)
                            .map(NotificacionLectura::isLeida).orElse(false);
                    return NotificacionResponse.fromEntity(n, leida);
                });
    }

    @Transactional
    public void eliminar(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion no encontrada"));
        notificacionRepository.delete(notificacion);
        auditoriaService.registrar("ELIMINAR_NOTIFICACION", "NOTIFICACIONES", notificacion.getTitulo(), null);
    }
}
