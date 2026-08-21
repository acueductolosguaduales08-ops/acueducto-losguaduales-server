package com.acueducto.backend.controller;

import com.acueducto.backend.dto.request.*;
import com.acueducto.backend.dto.response.AsociadoResponse;
import com.acueducto.backend.dto.response.LoginResponse;
import com.acueducto.backend.dto.response.UsuarioResponse;
import com.acueducto.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "01. Autenticacion", description = "Login, refresco de token y gestion de cuentas (Asociado, Tesorero, Administrador)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesion", description = "Autentica a un Asociado, Tesorero o Administrador y retorna un JWT (2.3).")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refrescar token de acceso")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Cerrar sesion", description = "Finaliza la sesion activa (2.3).")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear una cuenta de usuario", description = "Solo el Administrador puede crear cuentas de Tesorero o Administrador; el Tesorero puede crear cuentas de Asociado.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('TESORERO')")
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.ok(authService.crearUsuario(request));
    }

    @Operation(summary = "Perfil del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> perfil(Authentication authentication) {
        return ResponseEntity.ok(authService.obtenerPerfil(authentication.getName()));
    }

    @Operation(summary = "Cambiar contrasena propia")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(Authentication authentication, @Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar todas las cuentas", description = "Exclusivo del Administrador. Pide reconfirmar la contrasena aunque ya haya "
            + "iniciado sesion. Nunca incluye contrasenas: estan guardadas como hash de un solo sentido (BCrypt) y no se pueden mostrar, "
            + "ni siquiera por el propio Administrador; si el objetivo es ayudar a alguien que perdio su acceso, la accion correcta es "
            + "restablecer su contrasena (endpoint de creacion de cuenta / cambio de contrasena), no consultarla.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/usuarios/listar")
    public ResponseEntity<java.util.List<UsuarioResponse>> listarUsuarios(Authentication authentication,
                                                                           @Valid @RequestBody ConfirmarPasswordRequest request) {
        return ResponseEntity.ok(authService.listarUsuarios(authentication.getName(), request.password()));
    }

    @Operation(summary = "Activar o bloquear una cuenta", description = "Exclusivo del Administrador. Activa o bloquea una cuenta de usuario. "
            + "El primer administrador (id=1) nunca puede ser bloqueado. Se requiere la contrasena del administrador autenticado y un motivo al bloquear.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstadoCuenta(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CambiarEstadoCuentaRequest request) {
        return ResponseEntity.ok(authService.cambiarEstadoCuenta(id, authentication.getName(), request));
    }

    @Operation(summary = "Actualizar mis datos de asociado", description = "Permite al asociado autenticado actualizar sus propios datos personales. "
            + "La funcion debe estar habilitada por el Administrador. Puede editar: tipo de documento, documento, fecha de nacimiento, "
            + "telefonos, correo, direccion y barrio/vereda. NO puede editar: medidor, nombre, apellidos ni fecha de afiliacion.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ASOCIADO')")
    @PutMapping("/perfil/datos")
    public ResponseEntity<AsociadoResponse> actualizarDatosAsociado(
            Authentication authentication,
            @Valid @RequestBody ActualizarDatosAsociadoRequest request) {
        return ResponseEntity.ok(authService.actualizarDatosAsociado(authentication.getName(), request));
    }
}
