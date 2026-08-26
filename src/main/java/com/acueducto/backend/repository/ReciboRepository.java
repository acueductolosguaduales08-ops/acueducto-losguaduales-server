package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Recibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {
    Optional<Recibo> findByNumeroRecibo(String numeroRecibo);
    Optional<Recibo> findByPagoId(Long pagoId);
    Optional<Recibo> findByPagoMultaId(Long multaId);
    Page<Recibo> findByAsociadoId(Long asociadoId, Pageable pageable);
    List<Recibo> findAllByOrderByFechaEmisionDesc();

    @Query(value = "SELECT CAST(SUBSTRING(numero_recibo FROM 5) AS BIGINT) FROM recibo ORDER BY CAST(SUBSTRING(numero_recibo FROM 5) AS BIGINT) DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findMaxNumeroRecibo();
}
