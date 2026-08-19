package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByModulo(String modulo, Pageable pageable);
    Page<Auditoria> findByUsuario(String usuario, Pageable pageable);
}
