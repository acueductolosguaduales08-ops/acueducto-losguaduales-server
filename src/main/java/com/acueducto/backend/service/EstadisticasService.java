package com.acueducto.backend.service;

import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * Retorna la tendencia de recaudo de los ultimos N meses.
     * Cada elemento contiene: mes (string), ingresos, egresos, balance.
     */
    public List<Map<String, Object>> tendenciaRecaudo(int meses) {
        List<Map<String, Object>> tendencia = new ArrayList<>();
        LocalDate ahora = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = meses - 1; i >= 0; i--) {
            LocalDate mesInicio = ahora.minusMonths(i).withDayOfMonth(1);
            LocalDate mesFin = mesInicio.plusMonths(1).minusDays(1);
            LocalDateTime inicio = mesInicio.atStartOfDay();
            LocalDateTime fin = mesFin.atTime(23, 59, 59);

            var movimientos = movimientoTesoreriaRepository.findByFechaBetweenAndAnuladoFalse(inicio, fin);
            BigDecimal ingresos = movimientos.stream()
                    .filter(m -> m.getTipo() == TipoMovimiento.ENTRADA)
                    .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal egresos = movimientos.stream()
                    .filter(m -> m.getTipo() == TipoMovimiento.SALIDA)
                    .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("mes", mesInicio.format(fmt));
            punto.put("ingresos", ingresos);
            punto.put("egresos", egresos);
            punto.put("balance", ingresos.subtract(egresos));
            tendencia.add(punto);
        }
        return tendencia;
    }
}
