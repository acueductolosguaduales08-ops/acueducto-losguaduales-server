package com.acueducto.backend.repository;

import com.acueducto.backend.entity.NotaCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {
    Optional<NotaCredito> findByNumeroNota(String numeroNota);
    Page<NotaCredito> findByAsociadoId(Long asociadoId, Pageable pageable);
    List<NotaCredito> findByFacturaId(Long facturaId);
    List<NotaCredito> findAllByOrderByFechaEmisionDesc();

    @Query(value = "SELECT CAST(SUBSTRING(numero_nota FROM 4) AS BIGINT) FROM notas_credito ORDER BY CAST(SUBSTRING(numero_nota FROM 4) AS BIGINT) DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findMaxNumeroNota();
}
