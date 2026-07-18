package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.ReporteCiudadanoRequest;
import com.acueducto.backend.dto.response.ReporteCiudadanoResponse;
import com.acueducto.backend.entity.ReporteCiudadano;
import com.acueducto.backend.repository.ReporteCiudadanoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Modulo independiente de reportes ciudadanos: permite reportar una fuga o enviar una
 * queja/reclamo sin necesidad de iniciar sesion. Los reportes son temporales y se
 * eliminan automaticamente 8 dias despues de haber sido creados
 * (ver TareasProgramadasService.eliminarReportesCiudadanosVencidos).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteCiudadanoService {

    private static final int DIAS_RETENCION = 8;

    private final ReporteCiudadanoRepository reporteCiudadanoRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public ReporteCiudadanoResponse crear(ReporteCiudadanoRequest request) {
        LocalDateTime ahora = LocalDateTime.now();

        ReporteCiudadano reporte = ReporteCiudadano.builder()
                .nombre(request.nombre())
                .mensaje(request.mensaje())
                .contacto(request.contacto())
                .fechaEliminacion(ahora.plusDays(DIAS_RETENCION))
                .build();

        reporte = reporteCiudadanoRepository.save(reporte);

        notificacionService.notificarNuevoReporteCiudadano(reporte);

        return ReporteCiudadanoResponse.fromEntity(reporte);
    }

    public Page<ReporteCiudadanoResponse> listarTodos(Pageable pageable) {
        return reporteCiudadanoRepository.findAllByOrderByFechaCreacionDesc(pageable)
                .map(ReporteCiudadanoResponse::fromEntity);
    }

    /** Elimina definitivamente los reportes cuya fecha de eliminacion ya se cumplio. Retorna cuantos borro. */
    @Transactional
    public int eliminarVencidos() {
        var vencidos = reporteCiudadanoRepository.findByFechaEliminacionBefore(LocalDateTime.now());
        if (vencidos.isEmpty()) return 0;
        reporteCiudadanoRepository.deleteAll(vencidos);
        return vencidos.size();
    }
}
