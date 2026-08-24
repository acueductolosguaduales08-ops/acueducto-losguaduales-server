package com.acueducto.backend.repository;

import com.acueducto.backend.entity.NotificacionLectura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NotificacionLecturaRepository extends JpaRepository<NotificacionLectura, Long> {
    Optional<NotificacionLectura> findByNotificacionIdAndUsuarioId(Long notificacionId, Long usuarioId);
    List<NotificacionLectura> findByUsuarioId(Long usuarioId);
    List<NotificacionLectura> findByNotificacionId(Long notificacionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificacionLectura nl WHERE nl.notificacion.id = :notificacionId")
    int deleteByNotificacionId(@Param("notificacionId") Long notificacionId);
}
