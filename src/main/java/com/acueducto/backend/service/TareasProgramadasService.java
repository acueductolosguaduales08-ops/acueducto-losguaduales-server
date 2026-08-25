package com.acueducto.backend.service;

import com.acueducto.backend.entity.Asociado;
import com.acueducto.backend.entity.Configuracion;
import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Tareas automaticas del sistema: vencimiento de facturas, recordatorios de pago, suspension automatica, publicacion programada de contenido, encuestas/formularios programados, y limpieza de datos antiguos. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TareasProgramadasService {

    private final FacturaService facturaService;
    private final ReporteCiudadanoService reporteCiudadanoService;
    private final EncuestaService encuestaService;
    private final ChatService chatService;
    private final EnlacePublicoService enlacePublicoService;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final FacturaRepository facturaRepository;
    private final ConfiguracionService configuracionService;
    private final AsociadoService asociadoService;

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
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void sincronizarEncuestasProgramadas() {
        encuestaService.sincronizarTodas();
    }

    /** Se ejecuta una vez al dia a las 01:00 y elimina registros de auditoria con mas de 90 dias. */
    @Scheduled(cron = "0 0 1 * * *")
    public void limpiarAuditoriaAntigua() {
        int eliminados = auditoriaService.eliminarAntiguos(90);
        if (eliminados > 0) {
            log.info("Se eliminaron {} registro(s) de auditoria con mas de 90 dias.", eliminados);
        }
    }

    /** Se ejecuta una vez al dia a las 01:10 y elimina notificaciones vencidas + sus registros de lectura. */
    @Scheduled(cron = "0 10 1 * * *")
    public void limpiarNotificacionesVencidas() {
        int eliminadas = notificacionService.eliminarVencidas();
        if (eliminadas > 0) {
            log.info("Se eliminaron {} notificacion(es) vencida(s) y sus registros de lectura.", eliminadas);
        }
    }

    /**
     * Recordatorios automaticos de pago: se ejecuta diario a las 8am.
     * Notifica a asociados con facturas proximas a vencer (3 dias antes).
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void enviarRecordatoriosPago() {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(3);

        List<Factura> facturasProximasAVencer = facturaRepository
                .findByEstadoAndFechaLimitePagoBetween(EstadoFactura.PENDIENTE, hoy, fechaLimite);

        int enviados = 0;
        for (Factura factura : facturasProximasAVencer) {
            try {
                notificacionService.notificarRecordatorioPago(factura);
                enviados++;
            } catch (Exception e) {
                log.warn("No se pudo enviar recordatorio para factura {}: {}", factura.getNumeroFactura(), e.getMessage());
            }
        }
        if (enviados > 0) {
            log.info("Se enviaron {} recordatorio(s) de pago para facturas proximas a vencer.", enviados);
        }
    }

    /**
     * Suspension automatica de servicio: se ejecuta diario a las 00:25.
     * Revisa facturas vencidas con mas de X dias (configurable) y suspende
     * el servicio de los asociados que no han pagado.
     */
    @Scheduled(cron = "0 25 0 * * *")
    public void verificarSuspensionesAutomaticas() {
        Configuracion config = configuracionService.obtenerEntidad();
        int diasParaSuspension = config.getDiasParaSuspension() != null ? config.getDiasParaSuspension() : 30;

        LocalDate fechaLimiteSuspension = LocalDate.now().minusDays(diasParaSuspension);

        List<Factura> facturasVencidas = facturaRepository
                .findByEstadoAndFechaLimitePagoBefore(EstadoFactura.VENCIDA, fechaLimiteSuspension);

        // Agrupar por asociado
        var asociadosConFacturasVencidas = facturasVencidas.stream()
                .map(Factura::getAsociado)
                .distinct()
                .filter(a -> a.getEstadoServicio() == EstadoServicio.ACTIVO)
                .toList();

        int suspendidos = 0;
        for (Asociado asociado : asociadosConFacturasVencidas) {
            try {
                // Verificar que realmente tiene facturas vencidas por mas de X dias
                long facturasVencidasAntiguas = facturasVencidas.stream()
                        .filter(f -> f.getAsociado().getId().equals(asociado.getId()))
                        .count();

                if (facturasVencidasAntiguas > 0) {
                    notificacionService.notificarCorteProgramado(asociado, 0);
                    // Aqui se podria cambiar el estado a SUSPENDIDO directamente:
                    // asociadoService.cambiarEstadoServicio(asociado.getId(),
                    //     new CambioEstadoServicioRequest(EstadoServicio.SUSPENDIDO,
                    //     "Suspension automatica por " + facturasVencidasAntiguas + " factura(s) vencida(s)"));
                    suspendidos++;
                }
            } catch (Exception e) {
                log.warn("No se pudo procesar suspension para asociado {}: {}", asociado.getCodigoInterno(), e.getMessage());
            }
        }
        if (suspendidos > 0) {
            log.info("Se detectaron {} asociado(s) elegibles para suspension automatica.", suspendidos);
        }
    }
}
