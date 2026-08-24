package com.acueducto.backend.repository;

import com.acueducto.backend.entity.SolicitudEliminacion;
import com.acueducto.backend.entity.enums.EstadoSolicitudEliminacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudEliminacionRepository extends JpaRepository<SolicitudEliminacion, Long> {

    boolean existsByMensajeIdAndEstado(Long mensajeId, EstadoSolicitudEliminacion estado);

    /** Solicitudes pendientes que el usuario debe confirmar (como confirmador). */
    List<SolicitudEliminacion> findByConfirmadorIdAndEstado(Long confirmadorId, EstadoSolicitudEliminacion estado);

    /** Solicitudes pendientes que el usuario envio (como solicitante). */
    List<SolicitudEliminacion> findBySolicitanteIdAndEstado(Long solicitanteId, EstadoSolicitudEliminacion estado);

    /** Indica si existe una solicitud pendiente del usuario dentro de una conversacion. */
    boolean existsBySolicitanteIdAndEstadoAndMensaje_ConversacionId(Long solicitanteId, EstadoSolicitudEliminacion estado, Long conversacionId);

    boolean existsByConfirmadorIdAndEstadoAndMensaje_ConversacionId(Long confirmadorId, EstadoSolicitudEliminacion estado, Long conversacionId);

    /** Elimina todas las solicitudes asociadas a un mensaje (aceptacion o retencion de 8 dias). */
    @Modifying
    @Query("delete from SolicitudEliminacion s where s.mensaje.id = :mensajeId")
    int deleteByMensajeId(@Param("mensajeId") Long mensajeId);

    /** Elimina las solicitudes de un grupo de mensajes (retencion de 8 dias). */
    @Modifying
    @Query("delete from SolicitudEliminacion s where s.mensaje.id in :ids")
    int deleteByMensajeIdIn(@Param("ids") List<Long> ids);

    /** Elimina todas las solicitudes de los mensajes de una conversacion (borrado de conversacion). */
    @Modifying
    @Query("delete from SolicitudEliminacion s where s.mensaje.conversacion.id = :conversacionId")
    int deleteByMensaje_ConversacionId(@Param("conversacionId") Long conversacionId);

    /** Elimina todas las solicitudes donde el usuario es solicitante o confirmador. */
    @Modifying
    @Query("delete from SolicitudEliminacion s where s.solicitante.id = :usuarioId or s.confirmador.id = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}