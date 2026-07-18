package com.acueducto.backend.repository;

import com.acueducto.backend.entity.RespuestaEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespuestaEncuestaRepository extends JpaRepository<RespuestaEncuesta, Long> {
    List<RespuestaEncuesta> findByEncuestaId(Long encuestaId);
    boolean existsByEncuestaIdAndUsuarioId(Long encuestaId, Long usuarioId);
    long countByEncuestaId(Long encuestaId);
}
