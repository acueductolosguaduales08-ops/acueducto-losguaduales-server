package com.acueducto.backend.repository;

import com.acueducto.backend.entity.MovimientoTesoreria;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoTesoreriaRepository extends JpaRepository<MovimientoTesoreria, Long> {
    Optional<MovimientoTesoreria> findTopByTipoOrderByNumeroDesc(TipoMovimiento tipo);
    Page<MovimientoTesoreria> findByTipo(TipoMovimiento tipo, Pageable pageable);
    List<MovimientoTesoreria> findByFechaBetweenAndAnuladoFalse(LocalDateTime desde, LocalDateTime hasta);
    List<MovimientoTesoreria> findByMesContableId(Long mesContableId);
}
