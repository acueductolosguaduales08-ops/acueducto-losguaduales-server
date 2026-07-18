package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Publicacion;
import com.acueducto.backend.entity.enums.EstadoPublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    Page<Publicacion> findByEstado(EstadoPublicacion estado, Pageable pageable);
    List<Publicacion> findByEstadoAndDestacadaTrue(EstadoPublicacion estado);
}
