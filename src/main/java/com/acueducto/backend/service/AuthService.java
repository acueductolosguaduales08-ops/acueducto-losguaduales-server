package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.CambiarPasswordRequest;
import com.acueducto.backend.dto.request.CrearUsuarioRequest;
import com.acueducto.backend.dto.request.LoginRequest;
import com.acueducto.backend.dto.response.LoginResponse;
import com.acueducto.backend.dto.response.UsuarioResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.Rol;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.repository.UsuarioRepository;
import com.acueducto.backend.security.JwtService;
import com.acueducto.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Autenticacion y gestion de cuentas. El sistema soporta login unicamente para
 * Asociado, Tesorero y Administrador (2.3); el Usuario Publico no requiere cuenta.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final AsociadoRepository asociadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario se encuentra inactivo. Contacte al administrador.");
        }

        UserPrincipal principal = new UserPrincipal(usuario);
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().name());
        claims.put("userId", usuario.getId());

        String accessToken = jwtService.generateToken(principal, claims);
        String refreshToken = jwtService.generateRefreshToken(principal);

        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        auditoriaService.registrar("INICIO_SESION", "AUTENTICACION", usuario.getUsername(), null);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .usuario(UsuarioResponse.fromEntity(usuario))
                .build();
    }

    public void logout(String username) {
        auditoriaService.registrar("CIERRE_SESION", "AUTENTICACION", username, null);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        UserPrincipal principal = new UserPrincipal(usuario);
        if (!jwtService.isTokenValid(refreshToken, principal)) {
            throw new ReglaNegocioException("El refresh token es invalido o ha expirado. Inicie sesion nuevamente.");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().name());
        claims.put("userId", usuario.getId());
        String newAccessToken = jwtService.generateToken(principal, claims);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .usuario(UsuarioResponse.fromEntity(usuario))
                .build();
    }

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new RecursoDuplicadoException("El nombre de usuario ya esta en uso.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RecursoDuplicadoException("El correo ya esta registrado.");
        }

        Asociado asociado = null;
        if (request.rol() == Rol.ASOCIADO) {
            if (request.asociadoId() == null) {
                throw new ReglaNegocioException("Debe indicar el asociado al crear una cuenta con rol ASOCIADO.");
            }
            asociado = asociadoRepository.findById(request.asociadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"));
        }

        Usuario usuario = Usuario.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .rol(request.rol())
                .activo(true)
                .asociado(asociado)
                .build();

        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar("CREAR_USUARIO", "AUTENTICACION", usuario.getUsername(), "Rol: " + usuario.getRol());
        return UsuarioResponse.fromEntity(usuario);
    }

    @Transactional
    public void cambiarPassword(String username, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new ReglaNegocioException("La contrasena actual no es correcta.");
        }

        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
        auditoriaService.registrar("CAMBIO_PASSWORD", "AUTENTICACION", username, null);
    }

    public UsuarioResponse obtenerPerfil(String username) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return UsuarioResponse.fromEntity(usuario);
    }
}
