package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.HeroActualResponse;
import com.acueducto.backend.dto.response.HeroLinkResponse;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.entity.HeroLink;
import com.acueducto.backend.entity.enums.ModoHero;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.ConfiguracionRepository;
import com.acueducto.backend.repository.HeroLinkRepository;
import com.acueducto.backend.util.UrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Hero/banner del portal publico o la app (punto 1 del pedido). Puede haber varios links
 * registrados; segun el modo activo (Configuracion.modoHero) se muestra:
 * - UNICO: siempre el que este marcado como "principal".
 * - ALEATORIO_15MIN: uno al azar de los registrados, que cambia cada 15 minutos.
 *
 * El modo ALEATORIO_15MIN se resuelve "al leer" (en heroActual()), NO con una tarea de fondo
 * (@Scheduled): este proyecto esta desplegado en Render, cuyo plan gratuito apaga el servicio
 * tras ~15 minutos sin trafico, y una tarea programada simplemente no corre mientras el
 * servicio esta dormido. Calculando el hero vigente en el momento en que alguien lo consulta
 * (guardando cual toco y desde cuando) el resultado es correcto sin importar si el servicio
 * estuvo dormido en el medio.
 */
@Service
@RequiredArgsConstructor
public class HeroLinkService {

    private static final long MINUTOS_ROTACION = 15;

    private final HeroLinkRepository heroLinkRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final AuditoriaService auditoriaService;
    private final Random random = new Random();

    @Transactional
    public HeroLinkResponse agregar(String link) {
        String urlNormalizada = UrlUtil.normalizar(link);
        boolean esElPrimero = heroLinkRepository.count() == 0;

        HeroLink heroLink = HeroLink.builder()
                .link(urlNormalizada)
                // El primero registrado queda como principal automaticamente, para que el
                // modo UNICO siempre tenga algo que mostrar apenas se agrega un hero.
                .principal(esElPrimero)
                .build();
        heroLink = heroLinkRepository.save(heroLink);

        auditoriaService.registrar("AGREGAR_HERO", "CONFIGURACION", "hero_link:" + heroLink.getId(), null);
        return HeroLinkResponse.fromEntity(heroLink);
    }

    public List<HeroLinkResponse> listarTodos() {
        return heroLinkRepository.findAll().stream().map(HeroLinkResponse::fromEntity).toList();
    }

    @Transactional
    public void eliminar(Long id) {
        HeroLink heroLink = heroLinkRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Hero no encontrado con id " + id));
        boolean eraPrincipal = heroLink.isPrincipal();
        heroLinkRepository.delete(heroLink);

        // Si se borro el principal y quedan otros, se promueve otro automaticamente para que
        // el modo UNICO no se quede sin nada que mostrar.
        if (eraPrincipal) {
            heroLinkRepository.findAll().stream().findFirst().ifPresent(otro -> {
                otro.setPrincipal(true);
                heroLinkRepository.save(otro);
            });
        }

        // Si justo era el que estaba de turno en modo aleatorio, se limpia la marca para que
        // la proxima consulta elija uno nuevo en vez de intentar leer un id que ya no existe.
        Configuracion config = obtenerOCrearConfiguracion();
        if (id.equals(config.getHeroRotacionActualId())) {
            config.setHeroRotacionActualId(null);
            config.setHeroRotacionDesde(null);
            configuracionRepository.save(config);
        }

        auditoriaService.registrar("ELIMINAR_HERO", "CONFIGURACION", "hero_link:" + id, null);
    }

    @Transactional
    public HeroLinkResponse elegirPrincipal(Long id) {
        HeroLink nuevoPrincipal = heroLinkRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Hero no encontrado con id " + id));

        heroLinkRepository.findByPrincipalTrue().ifPresent(actual -> {
            if (!actual.getId().equals(id)) {
                actual.setPrincipal(false);
                heroLinkRepository.save(actual);
            }
        });

        nuevoPrincipal.setPrincipal(true);
        nuevoPrincipal = heroLinkRepository.save(nuevoPrincipal);

        auditoriaService.registrar("ELEGIR_HERO_PRINCIPAL", "CONFIGURACION", "hero_link:" + id, null);
        return HeroLinkResponse.fromEntity(nuevoPrincipal);
    }

    @Transactional
    public void cambiarModo(ModoHero nuevoModo) {
        Configuracion config = obtenerOCrearConfiguracion();
        config.setModoHero(nuevoModo);
        // Se limpia el estado de rotacion para que, si se vuelve a modo aleatorio mas
        // adelante, se elija un hero nuevo de inmediato en vez de reusar una eleccion vieja.
        config.setHeroRotacionActualId(null);
        config.setHeroRotacionDesde(null);
        configuracionRepository.save(config);
        auditoriaService.registrar("CAMBIAR_MODO_HERO", "CONFIGURACION", "modo_hero", nuevoModo.name());
    }

    /** Publico: el hero que corresponde mostrar ahora mismo, segun el modo activo. */
    @Transactional
    public HeroActualResponse heroActual() {
        Configuracion config = obtenerOCrearConfiguracion();
        ModoHero modo = config.getModoHero();

        if (modo == ModoHero.UNICO) {
            String link = heroLinkRepository.findByPrincipalTrue().map(HeroLink::getLink).orElse(null);
            return new HeroActualResponse(link, modo);
        }

        // ALEATORIO_15MIN
        List<HeroLink> todos = heroLinkRepository.findAll();
        if (todos.isEmpty()) {
            return new HeroActualResponse(null, modo);
        }

        boolean vencido = config.getHeroRotacionDesde() == null
                || LocalDateTime.now().isAfter(config.getHeroRotacionDesde().plusMinutes(MINUTOS_ROTACION));

        Optional<HeroLink> actual = config.getHeroRotacionActualId() != null
                ? todos.stream().filter(h -> h.getId().equals(config.getHeroRotacionActualId())).findFirst()
                : Optional.empty();

        if (vencido || actual.isEmpty()) {
            HeroLink elegido = todos.get(random.nextInt(todos.size()));
            config.setHeroRotacionActualId(elegido.getId());
            config.setHeroRotacionDesde(LocalDateTime.now());
            configuracionRepository.save(config);
            return new HeroActualResponse(elegido.getLink(), modo);
        }

        return new HeroActualResponse(actual.get().getLink(), modo);
    }

    private Configuracion obtenerOCrearConfiguracion() {
        return configuracionRepository.findAll().stream().findFirst()
                .orElseGet(() -> configuracionRepository.save(Configuracion.builder().build()));
    }
}
