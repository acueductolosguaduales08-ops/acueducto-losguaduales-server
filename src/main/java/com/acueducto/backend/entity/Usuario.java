package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.Rol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario con acceso autenticado al sistema (Asociado, Tesorero o Administrador).
 * El "Usuario Publico" no requiere registro ni autenticacion.
 */
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Solo aplica cuando rol = ASOCIADO: vincula la cuenta con su expediente de asociado. */
    @OneToOne
    @JoinColumn(name = "asociado_id")
    private Asociado asociado;

    @Column(name = "ultimo_login")
    private java.time.LocalDateTime ultimoLogin;
}
