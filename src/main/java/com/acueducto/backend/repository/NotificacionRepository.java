package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Notificacion;
import com.acueducto.backend.entity.enums.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    Page<Notificacion> findByTipo(TipoNotificacion tipo, Pageable pageable);
    Page<Notificacion> findByDestinatarioId(Long usuarioId, Pageable pageable);
    List<Notificacion> findByDestinatarioId(Long usuarioId);
    List<Notificacion> findByAutorId(Long autorId);
    boolean existsByAutorId(Long autorId);
}
