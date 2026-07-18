package com.acueducto.backend.repository;

import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Usuario> findByAsociadoId(Long asociadoId);
    List<Usuario> findByRolInAndActivoTrue(List<Rol> roles);
}
