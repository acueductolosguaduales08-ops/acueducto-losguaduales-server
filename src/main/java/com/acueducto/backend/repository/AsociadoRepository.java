package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.enums.EstadoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AsociadoRepository extends JpaRepository<Asociado, Long> {
    Optional<Asociado> findByDocumento(String documento);
    boolean existsByDocumento(String documento);

    @Query("""
           select a from Asociado a where a.archivado = false and (
             lower(a.documento) like lower(concat('%', :q, '%')) or
             lower(a.nombres) like lower(concat('%', :q, '%')) or
             lower(a.apellidos) like lower(concat('%', :q, '%')) or
             lower(a.telefonoPrincipal) like lower(concat('%', :q, '%'))
           )""")
    java.util.List<Asociado> buscar(@Param("q") String query);

    java.util.List<Asociado> findByEstadoServicioAndArchivadoFalse(EstadoServicio estado);
}
