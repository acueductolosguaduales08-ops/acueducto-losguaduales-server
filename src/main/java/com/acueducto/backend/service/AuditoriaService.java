package com.acueducto.backend.service;

import com.acueducto.backend.entity.Auditoria;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.repository.AuditoriaRepository;
import com.acueducto.backend.repository.ConfiguracionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Registra automaticamente las acciones importantes del sistema para garantizar
 * trazabilidad y transparencia (2.13 / 4.4). Solo el Administrador puede desactivar
 * temporalmente el registro de auditoria; esa misma accion siempre queda registrada
 * como el ultimo movimiento antes de detenerse, dejando constancia de quien la solicito.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final ConfiguracionRepository configuracionRepository;

    public void registrar(String accion, String modulo, String registroAfectado, String observaciones) {
        if (!auditoriaActiva()) return;
        guardar(accion, modulo, registroAfectado, observaciones);
    }

    private void guardar(String accion, String modulo, String registroAfectado, String observaciones) {
        Auditoria auditoria = Auditoria.builder()
                .usuario(usuarioActual())
                .fecha(LocalDateTime.now())
                .accion(accion)
                .modulo(modulo)
                .registroAfectado(registroAfectado)
                .ip(ipActual())
                .observaciones(observaciones)
                .build();
        auditoriaRepository.save(auditoria);
    }

    private boolean auditoriaActiva() {
        return configuracionRepository.findAll().stream().findFirst()
                .map(Configuracion::isAuditoriaActiva)
                .orElse(true);
    }

    /** Desactiva el registro de auditoria. La desactivacion misma siempre queda registrada (4.4). */
    @Transactional
    public void desactivar(String nombreResponsable) {
        guardar("AUDITORIA_DESACTIVADA", "AUDITORIA", null,
                "Auditoria desactivada por: " + nombreResponsable + " (usuario de sesion: " + usuarioActual() + ")");

        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException(
                        "La configuracion del sistema aun no ha sido inicializada."));
        config.setAuditoriaActiva(false);
        configuracionRepository.save(config);
    }

    /** Reactiva el registro de auditoria; el evento de reactivacion siempre queda registrado. */
    @Transactional
    public void activar(String nombreResponsable) {
        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException(
                        "La configuracion del sistema aun no ha sido inicializada."));
        config.setAuditoriaActiva(true);
        configuracionRepository.save(config);

        guardar("AUDITORIA_ACTIVADA", "AUDITORIA", null,
                "Auditoria reactivada por: " + nombreResponsable + " (usuario de sesion: " + usuarioActual() + ")");
    }

    public boolean estaActiva() {
        return auditoriaActiva();
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "publico";
        }
        return auth.getName();
    }

    private String ipActual() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return forwarded != null ? forwarded : request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}

