package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoFactura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByNumeroFactura(String numeroFactura);
    boolean existsByAsociadoIdAndMesContableId(Long asociadoId, Long mesContableId);
    Page<Factura> findByAsociadoId(Long asociadoId, Pageable pageable);
    Page<Factura> findByEstado(EstadoFactura estado, Pageable pageable);
    List<Factura> findByMesContableId(Long mesContableId);
    List<Factura> findByEstadoAndFechaLimitePagoBefore(EstadoFactura estado, java.time.LocalDate fecha);
}
