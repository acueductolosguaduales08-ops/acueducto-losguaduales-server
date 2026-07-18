package com.acueducto.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper usado en @PreAuthorize para garantizar que un Asociado solo consulte su propia
 * informacion (2.8 / 3.4): nunca la de otro asociado, incluso conociendo el id por URL.
 */
@Component("asociadoSecurity")
@RequiredArgsConstructor
public class AsociadoSecurity {

    public boolean esPropio(Long asociadoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        var usuario = principal.getUsuario();
        return usuario.getAsociado() != null && usuario.getAsociado().getId().equals(asociadoId);
    }
}
