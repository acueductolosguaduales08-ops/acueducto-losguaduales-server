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

        // La imagen es opcional; si viene, se normaliza igual que cualquier otro link del
        // sistema (acepta URLs largas y con caracteres especiales sin romper el guardado).
        String imagenNormalizada = (request.imagenUrl() != null && !request.imagenUrl().isBlank())
                ? com.acueducto.backend.util.UrlUtil.normalizar(request.imagenUrl())
                : null;

        ReporteCiudadano reporte = ReporteCiudadano.builder()
                .nombre(request.nombre())
                .mensaje(request.mensaje())
                .contacto(request.contacto())
                .imagenUrl(imagenNormalizada)
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

    /**
     * Borrado manual e inmediato, exclusivo del Administrador. Independiente del borrado
     * automatico por vencimiento (eliminarVencidos): este permite borrar un reporte antes de
     * que se cumplan los 8 dias (por ejemplo, spam o contenido inapropiado).
     */
    @Transactional
    public void eliminarDefinitivamente(Long id) {
        ReporteCiudadano reporte = reporteCiudadanoRepository.findById(id)
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException(
                        "Reporte ciudadano no encontrado con id " + id));
        reporteCiudadanoRepository.delete(reporte);
    }
}
