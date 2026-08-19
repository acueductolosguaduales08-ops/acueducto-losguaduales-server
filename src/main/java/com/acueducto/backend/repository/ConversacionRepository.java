package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    /** Busca la conversacion entre una pareja normalizada (usuario1 = id menor). */
    Optional<Conversacion> findByUsuario1IdAndUsuario2Id(Long usuario1Id, Long usuario2Id);

    /** Todas las conversaciones donde participa el usuario (como usuario1 o usuario2). */
    List<Conversacion> findByUsuario1IdOrUsuario2Id(Long usuario1Id, Long usuario2Id);
}