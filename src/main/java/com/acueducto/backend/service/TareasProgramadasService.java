package com.acueducto.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Tareas automaticas del sistema: vencimiento de facturas, publicacion programada de contenido y encuestas/formularios programados. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TareasProgramadasService {

    private final FacturaService facturaService;
    private final ReporteCiudadanoService reporteCiudadanoService;
    private final EncuestaService encuestaService;
    private final ChatService chatService;
    private final EnlacePublicoService enlacePublicoService;

    /** Se ejecuta una vez al dia a las 00:10 y marca como VENCIDA toda factura pendiente fuera de plazo (2.11 / 7.5). */
    @Scheduled(cron = "0 10 0 * * *")
    public void marcarFacturasVencidas() {
        int actualizadas = facturaService.marcarFacturasVencidas();
        if (actualizadas > 0) {
            log.info("Se marcaron {} factura(s) como VENCIDA por vencimiento de plazo de pago.", actualizadas);
        }
    }

    /** Se ejecuta una vez al dia a las 00:20 y elimina definitivamente los reportes ciudadanos con mas de 8 dias. */
    @Scheduled(cron = "0 20 0 * * *")
    public void eliminarReportesCiudadanosVencidos() {
        int eliminados = reporteCiudadanoService.eliminarVencidos();
        if (eliminados > 0) {
            log.info("Se eliminaron automaticamente {} reporte(s) ciudadano(s) por cumplir 8 dias.", eliminados);
        }
    }

    /** Se ejecuta una vez al dia a las 00:30 y elimina los mensajes de chat con mas de 8 dias (retencion PostgreSQL). */
    @Scheduled(cron = "0 30 0 * * *")
    public void eliminarMensajesChatAntiguos() {
        int eliminados = chatService.eliminarMensajesAntiguos();
        if (eliminados > 0) {
            log.info("Se eliminaron {} mensaje(s) de chat con mas de 8 dias de antiguedad.", eliminados);
        }
    }

    /** Se ejecuta una vez al dia a las 00:40 y borra definitivamente los enlaces publicos de facturas/recibos ya vencidos (72 horas). */
    @Scheduled(cron = "0 40 0 * * *")
    public void eliminarEnlacesPublicosVencidos() {
        enlacePublicoService.eliminarExpirados();
    }

    /**
     * Barrido "best-effort" cada 5 minutos: abre/cierra automaticamente los formularios
     * programados (fechaInicio/fechaFin) que ya deberian haber cambiado de estado.
     *
     * OJO: esto NO es lo unico que garantiza la programacion. Si el servicio esta dormido
     * (Render lo apaga tras inactividad) este barrido simplemente no corre, asi que
     * EncuestaService tambien recalcula el estado correcto "al leer" cada vez que alguien
     * consulta o usa una encuesta puntual (ver EncuestaService.sincronizarEstado). Este barrido
     * solo mejora la experiencia mientras el servicio esta despierto (por ejemplo en horario
     * de uso activo), para que el cambio se note sin que alguien tenga que abrir esa encuesta
     * especifica primero.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void sincronizarEncuestasProgramadas() {
        encuestaService.sincronizarTodas();
    }
}
