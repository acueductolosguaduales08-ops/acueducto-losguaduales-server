package com.acueducto.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Tareas automaticas del sistema: vencimiento de facturas y publicacion programada de contenido. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TareasProgramadasService {

    private final FacturaService facturaService;
    private final ReporteCiudadanoService reporteCiudadanoService;

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
}
