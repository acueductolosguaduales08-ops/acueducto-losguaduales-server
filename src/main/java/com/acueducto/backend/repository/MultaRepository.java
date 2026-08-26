package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.entity.enums.EstadoMulta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MultaRepository extends JpaRepository<Multa, Long> {
    @Override
    @EntityGraph(attributePaths = {"asociado", "factura"})
    Optional<Multa> findById(Long id);

    List<Multa> findByAsociadoId(Long asociadoId);
    List<Multa> findByAsociadoIdAndEstado(Long asociadoId, EstadoMulta estado);
    List<Multa> findByFacturaId(Long facturaId);

    /** Usado por FacturaService: las multas independientes nunca se incluyen en una factura. */
    List<Multa> findByAsociadoIdAndEstadoAndIndependienteFalse(Long asociadoId, EstadoMulta estado);
}
