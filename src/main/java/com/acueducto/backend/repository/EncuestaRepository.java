package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Encuesta;
import com.acueducto.backend.entity.enums.EstadoEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EncuestaRepository extends JpaRepository<Encuesta, Long> {
    Optional<Encuesta> findByCodigo(String codigo);
    List<Encuesta> findByEstado(EstadoEncuesta estado);
    List<Encuesta> findByEstadoIn(List<EstadoEncuesta> estados);
    boolean existsByAutorId(Long autorId);
    List<Encuesta> findByAutorId(Long autorId);

    @Query(value = "SELECT CAST(SUBSTRING(codigo FROM 5) AS BIGINT) FROM encuestas ORDER BY CAST(SUBSTRING(codigo FROM 5) AS BIGINT) DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findMaxNumeroCodigo();
}
