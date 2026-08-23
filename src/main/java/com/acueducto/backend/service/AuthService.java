package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.ActualizarDatosAsociadoRequest;
import com.acueducto.backend.dto.request.CambiarEstadoCuentaRequest;
import com.acueducto.backend.dto.request.CambiarPasswordRequest;
import com.acueducto.backend.dto.request.CrearUsuarioRequest;
import com.acueducto.backend.dto.request.LoginRequest;
import com.acueducto.backend.dto.response.AsociadoResponse;
import com.acueducto.backend.dto.response.LoginResponse;
import com.acueducto.backend.dto.response.UsuarioResponse;
import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.entity.Usuario;
import com.acueducto.backend.entity.enums.Rol;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.repository.ConfiguracionRepository;
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
    private final ConfiguracionRepository configuracionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!usuario.isActivo() && usuario.getId() != 1L) {
            String motivo = usuario.getMotivoBloqueo() != null ? usuario.getMotivoBloqueo() : "";
            String mensaje = "Lo sentimos, tu cuenta ha sido bloqueada por uno de nuestros administradores."
                    + (motivo.isEmpty() ? "" : " Asunto: " + motivo);
            throw new ReglaNegocioException(mensaje);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

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

        if (!usuario.isActivo() && usuario.getId() != 1L) {
            String motivo = usuario.getMotivoBloqueo() != null ? usuario.getMotivoBloqueo() : "";
            String mensaje = "Lo sentimos, tu cuenta ha sido bloqueada por uno de nuestros administradores."
                    + (motivo.isEmpty() ? "" : " Asunto: " + motivo);
            throw new ReglaNegocioException(mensaje);
        }

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
        if (usuarioRepository.existsByContactoIgnoreCase(request.contacto())) {
            throw new RecursoDuplicadoException("El correo o telefono ya esta registrado.");
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
                .contacto(request.contacto())
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
        notificacionService.notificarCambioPassword(usuario);
    }

    public UsuarioResponse obtenerPerfil(String username) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return UsuarioResponse.fromEntity(usuario);
    }

    /**
     * Confirma la contrasena del usuario autenticado, aunque ya tenga sesion iniciada. Usado
     * por operaciones sensibles que piden reconfirmar identidad: listar cuentas (8) y todo el
     * modulo de eliminacion definitiva (5). La contrasena nunca se puede "mostrar" porque
     * usuario.getPassword() es un hash de un solo sentido (BCrypt); solo se puede verificar.
     */
    public void verificarPassword(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new ReglaNegocioException("La contrasena no es correcta.");
        }
    }

    /**
     * Listado de todas las cuentas del sistema (8). Exclusivo del Administrador, y pide
     * reconfirmar la contrasena aunque ya haya iniciado sesion. Nunca incluye la contrasena de
     * nadie: UsuarioResponse no tiene ese campo, y el hash guardado (BCrypt) no se puede
     * revertir a texto plano de todas formas, ni siquiera por el propio Administrador.
     */
    public java.util.List<UsuarioResponse> listarUsuarios(String usernameAdmin, String password) {
        verificarPassword(usernameAdmin, password);
        auditoriaService.registrar("LISTAR_CUENTAS", "AUTENTICACION", usernameAdmin, null);
        return usuarioRepository.findAll().stream().map(UsuarioResponse::fromEntity).toList();
    }

    /**
     * Activa o bloquea una cuenta de usuario. El primer administrador (id=1) nunca puede ser
     * bloqueado. Se requiere la contrasena del administrador autenticado. Al bloquear, se
     * almacena el motivo que se mostrara al usuario cuando intente iniciar sesion o si ya
     * tiene sesion activa.
     */
    @Transactional
    public UsuarioResponse cambiarEstadoCuenta(Long usuarioId, String usernameAdmin,
                                                CambiarEstadoCuentaRequest request) {
        verificarPassword(usernameAdmin, request.password());

        Usuario admin = usuarioRepository.findByUsernameIgnoreCase(usernameAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado"));

        if (admin.getRol() != Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException("Solo los administradores pueden cambiar el estado de cuentas.");
        }

        if (usuarioId.equals(admin.getId())) {
            throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (usuario.getId() == 1L) {
            throw new ReglaNegocioException("La cuenta del administrador principal no puede ser bloqueada.");
        }

        boolean bloqueando = usuario.isActivo();

        if (bloqueando) {
            usuario.setActivo(false);
            usuario.setMotivoBloqueo(request.motivo() != null ? request.motivo() : "");
            auditoriaService.registrar("BLOQUEAR_CUENTA", "AUTENTICACION", usuario.getUsername(),
                    "Motivo: " + (request.motivo() != null ? request.motivo() : "Sin motivo"));
        } else {
            usuario.setActivo(true);
            usuario.setMotivoBloqueo(null);
            auditoriaService.registrar("DESACTIVAR_BLOQUEO_CUENTA", "AUTENTICACION", usuario.getUsername(), null);
        }

        usuarioRepository.save(usuario);
        return UsuarioResponse.fromEntity(usuario);
    }

    /**
     * Permite a un asociado actualizar sus propios datos personales. La funcion debe estar
     * habilitada por el Administrador en Configuracion (edicionAsociadosActiva). Solo puede
     * editar: tipoDocumento, documento, fechaNacimiento, telefonoPrincipal, telefonoAlternativo,
     * correo, direccion, barrioVereda. NO puede editar: medidor, nombres, apellidos, fechaAfiliacion.
     */
    @Transactional
    public AsociadoResponse actualizarDatosAsociado(String username, ActualizarDatosAsociadoRequest request) {
        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElse(null);
        if (config != null && !config.isEdicionAsociadosActiva()) {
            throw new ReglaNegocioException("La edicion de datos por parte de asociados esta desactivada. Contacte al administrador.");
        }

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (usuario.getRol() != Rol.ASOCIADO || usuario.getAsociado() == null) {
            throw new ReglaNegocioException("Solo los asociados pueden editar sus propios datos.");
        }

        Asociado asociado = usuario.getAsociado();

        if (!asociado.getDocumento().equals(request.documento())
                && asociadoRepository.existsByDocumento(request.documento())) {
            throw new RecursoDuplicadoException("Ya existe un asociado registrado con este documento.");
        }

        asociado.setTipoDocumento(request.tipoDocumento());
        asociado.setDocumento(request.documento());
        asociado.setFechaNacimiento(request.fechaNacimiento());
        asociado.setTelefonoPrincipal(request.telefonoPrincipal());
        asociado.setTelefonoAlternativo(request.telefonoAlternativo());
        asociado.setCorreo(request.correo());
        asociado.setDireccion(request.direccion());
        asociado.setBarrioVereda(request.barrioVereda());

        asociado = asociadoRepository.save(asociado);
        auditoriaService.registrar("ACTUALIZAR_DATOS_ASOCIADO", "ASOCIADOS", asociado.getCodigoInterno(),
                "Autoedicion por el asociado");
        return AsociadoResponse.fromEntity(asociado, true);
    }
}
