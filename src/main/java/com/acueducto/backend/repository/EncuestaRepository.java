package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Encuesta;
import com.acueducto.backend.entity.enums.EstadoEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncuestaRepository extends JpaRepository<Encuesta, Long> {
    Optional<Encuesta> findByCodigo(String codigo);
    List<Encuesta> findByEstado(EstadoEncuesta estado);
}
