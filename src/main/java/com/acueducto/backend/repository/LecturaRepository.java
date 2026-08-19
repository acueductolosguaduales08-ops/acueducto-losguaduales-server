package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Lectura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturaRepository extends JpaRepository<Lectura, Long> {
    Optional<Lectura> findByMedidorIdAndMesContableId(Long medidorId, Long mesContableId);
    List<Lectura> findByAsociadoIdOrderByFechaLecturaDesc(Long asociadoId);
    List<Lectura> findByMesContableIdAndFacturaGeneradaFalse(Long mesContableId);
    List<Lectura> findByMesContableId(Long mesContableId);

    Optional<Lectura> findTopByMedidorIdOrderByFechaLecturaDesc(Long medidorId);
}
