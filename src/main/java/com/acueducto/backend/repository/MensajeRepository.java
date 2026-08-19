package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** Ultimo mensaje de una conversacion (para la lista de conversaciones). */
    Optional<Mensaje> findTopByConversacionIdOrderByIdDesc(Long conversacionId);

    /** Cantidad de mensajes no leidos de una conversacion para un usuario dado. */
    long countByConversacionIdAndRemitenteIdNotAndLeidoFalse(Long conversacionId, Long remitenteId);

    List<Mensaje> findByConversacionIdOrderByIdAsc(Long conversacionId);

    /** Mensajes posteriores a un id (sincronizacion incremental con IndexedDB). */
    List<Mensaje> findByConversacionIdAndIdGreaterThanOrderByIdAsc(Long conversacionId, Long id);

    /** Mensajes posteriores a una fecha (sincronizacion alternativa). */
    List<Mensaje> findByConversacionIdAndFechaCreacionGreaterThanOrderByIdAsc(Long conversacionId, LocalDateTime fecha);

    /** Marca como leidos los mensajes recibidos (remitente distinto al usuario) de la conversacion. */
    @Modifying
    @Query("update Mensaje m set m.leido = true, m.fechaLectura = :fecha "
            + "where m.conversacion.id = :conversacionId and m.remitente.id <> :usuarioId and m.leido = false")
    int marcarLeidosPorConversacion(@Param("conversacionId") Long conversacionId,
                                    @Param("usuarioId") Long usuarioId,
                                    @Param("fecha") LocalDateTime fecha);

    /** Elimina todos los mensajes de una conversacion (borrado completo de la conversacion). */
    @Modifying
    @Query("delete from Mensaje m where m.conversacion.id = :conversacionId")
    int deleteByConversacionId(@Param("conversacionId") Long conversacionId);

    /** Eliminacion masiva por ids (retencion de 8 dias). */
    @Modifying
    @Query("delete from Mensaje m where m.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    /** Ids de los mensajes creados antes de la fecha indicada (limpieza programada). */
    @Query("select m.id from Mensaje m where m.fechaCreacion < :fecha")
    List<Long> findIdsByFechaCreacionBefore(@Param("fecha") LocalDateTime fecha);
}