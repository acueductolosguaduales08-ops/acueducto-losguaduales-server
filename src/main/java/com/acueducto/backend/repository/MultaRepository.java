package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Multa;
import com.acueducto.backend.entity.enums.EstadoMulta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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

    long countByEstado(EstadoMulta estado);

    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Multa m WHERE m.estado = com.acueducto.backend.entity.enums.EstadoMulta.PENDIENTE")
    BigDecimal sumValorPendiente();
}
