package com.acueducto.backend.repository;

import com.acueducto.backend.entity.NotificacionLectura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionLecturaRepository extends JpaRepository<NotificacionLectura, Long> {
    Optional<NotificacionLectura> findByNotificacionIdAndUsuarioId(Long notificacionId, Long usuarioId);
    List<NotificacionLectura> findByUsuarioId(Long usuarioId);
}
