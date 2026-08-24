package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByModulo(String modulo, Pageable pageable);
    Page<Auditoria> findByUsuario(String usuario, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Auditoria a WHERE a.fecha < :fecha")
    int deleteByFechaBefore(@Param("fecha") LocalDateTime fecha);
}
