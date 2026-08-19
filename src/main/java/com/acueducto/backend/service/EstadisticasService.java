package com.acueducto.backend.service;

import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard general: indicadores administrativos y financieros en tiempo real (1.3 / 12.12).
 * No persiste nada; calcula todo bajo demanda a partir de los datos actuales.
 */
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final AsociadoRepository asociadoRepository;
    private final FacturaRepository facturaRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final EncuestaRepository encuestaRepository;

    public Map<String, Object> dashboardGeneral() {
        Map<String, Object> resultado = new LinkedHashMap<>();

        long asociadosActivos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.ACTIVO).size();
        long asociadosSuspendidos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.SUSPENDIDO).size();
        resultado.put("asociadosActivos", asociadosActivos);
        resultado.put("asociadosSuspendidos", asociadosSuspendidos);

        var facturasPendientes = facturaRepository.findByEstado(EstadoFactura.PENDIENTE, Pageable.unpaged());
        var facturasVencidas = facturaRepository.findByEstado(EstadoFactura.VENCIDA, Pageable.unpaged());
        var facturasPagadas = facturaRepository.findByEstado(EstadoFactura.PAGADA, Pageable.unpaged());
        resultado.put("facturasPendientes", facturasPendientes.getTotalElements());
        resultado.put("facturasVencidas", facturasVencidas.getTotalElements());
        resultado.put("facturasPagadas", facturasPagadas.getTotalElements());

        BigDecimal totalCarteraPendiente = facturasPendientes.getContent().stream()
                .map(com.acueducto.backend.entity.Factura::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(facturasVencidas.getContent().stream()
                        .map(com.acueducto.backend.entity.Factura::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add));
        resultado.put("totalCarteraPendiente", totalCarteraPendiente);

        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        var movimientosMes = movimientoTesoreriaRepository.findByFechaBetweenAndAnuladoFalse(inicioMes, LocalDateTime.now());
        BigDecimal ingresosMes = movimientosMes.stream().filter(m -> m.getTipo() == TipoMovimiento.ENTRADA)
                .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gastosMes = movimientosMes.stream().filter(m -> m.getTipo() == TipoMovimiento.SALIDA)
                .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        resultado.put("ingresosMesActual", ingresosMes);
        resultado.put("gastosMesActual", gastosMes);
        resultado.put("balanceMesActual", ingresosMes.subtract(gastosMes));

        resultado.put("encuestasActivas", encuestaRepository.findByEstado(
                com.acueducto.backend.entity.enums.EstadoEncuesta.ACTIVA).size());

        return resultado;
    }
}
