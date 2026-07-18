package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.InformeAsociadoResponse;
import com.acueducto.backend.dto.response.InformePeriodoResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulo de Informes: genera reportes financieros y administrativos para que el
 * Tesorero o el Administrador los presenten (mensual, anual, o de seguimiento a
 * un asociado especifico). Reutiliza los mismos logos institucionales que facturas
 * y recibos (via DocumentoService/Configuracion).
 */
@Service
@RequiredArgsConstructor
public class InformeService {

    private final MesContableRepository mesContableRepository;
    private final AnioContableRepository anioContableRepository;
    private final FacturaRepository facturaRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final MultaRepository multaRepository;
    private final LecturaRepository lecturaRepository;
    private final AsociadoRepository asociadoRepository;
    private final PagoRepository pagoRepository;
    private final ReciboRepository reciboRepository;

    // ================= INFORME MENSUAL =================

    public InformePeriodoResponse generarInformeMes(Long mesContableId) {
        MesContable mes = mesContableRepository.findById(mesContableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mes contable no encontrado"));

        List<Factura> facturas = facturaRepository.findByMesContableId(mesContableId);
        List<MovimientoTesoreria> movimientos = movimientoTesoreriaRepository.findByMesContableId(mesContableId);
        List<Lectura> lecturas = lecturaRepository.findByMesContableId(mesContableId);

        LocalDate inicio = LocalDate.of(mes.getAnioContable().getAnio(), mes.getNumeroMes(), 1);
        LocalDate fin = inicio.plusMonths(1).minusDays(1);
        List<Multa> multas = multaRepository.findAll().stream()
                .filter(m -> !m.getFecha().isBefore(inicio) && !m.getFecha().isAfter(fin))
                .toList();

        String titulo = mes.getNombreMes() + " " + mes.getAnioContable().getAnio();
        return construirInformePeriodo("Mensual", titulo, facturas, movimientos, multas, lecturas);
    }

    // ================= INFORME ANUAL =================

    public InformePeriodoResponse generarInformeAnio(Long anioContableId) {
        AnioContable anio = anioContableRepository.findById(anioContableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Año contable no encontrado"));

        List<MesContable> meses = mesContableRepository.findByAnioContableIdOrderByNumeroMes(anioContableId);

        List<Factura> facturas = new ArrayList<>();
        List<MovimientoTesoreria> movimientos = new ArrayList<>();
        List<Lectura> lecturas = new ArrayList<>();
        for (MesContable mes : meses) {
            facturas.addAll(facturaRepository.findByMesContableId(mes.getId()));
            movimientos.addAll(movimientoTesoreriaRepository.findByMesContableId(mes.getId()));
            lecturas.addAll(lecturaRepository.findByMesContableId(mes.getId()));
        }

        LocalDate inicio = LocalDate.of(anio.getAnio(), 1, 1);
        LocalDate fin = LocalDate.of(anio.getAnio(), 12, 31);
        List<Multa> multas = multaRepository.findAll().stream()
                .filter(m -> !m.getFecha().isBefore(inicio) && !m.getFecha().isAfter(fin))
                .toList();

        String titulo = "Año " + anio.getAnio();
        return construirInformePeriodo("Anual", titulo, facturas, movimientos, multas, lecturas);
    }

    private InformePeriodoResponse construirInformePeriodo(String tipoInforme, String titulo,
                                                             List<Factura> facturas,
                                                             List<MovimientoTesoreria> movimientos,
                                                             List<Multa> multas,
                                                             List<Lectura> lecturas) {

        long facturasPagadas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PAGADA).count();
        long facturasPendientes = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PENDIENTE || f.getEstado() == EstadoFactura.PAGADA_PARCIAL).count();
        long facturasVencidas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.VENCIDA).count();
        long facturasAnuladas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.ANULADA).count();

        BigDecimal totalFacturado = facturas.stream().map(Factura::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecaudadoFacturas = facturas.stream().map(Factura::getTotalPagado).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MovimientoTesoreria> movimientosVigentes = movimientos.stream().filter(m -> !m.isAnulado()).toList();
        BigDecimal totalIngresos = movimientosVigentes.stream().filter(m -> m.getTipo() == TipoMovimiento.ENTRADA)
                .map(MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGastos = movimientosVigentes.stream().filter(m -> m.getTipo() == TipoMovimiento.SALIDA)
                .map(MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        long numeroMultas = multas.size();
        BigDecimal totalMultas = multas.stream().map(Multa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalM3 = lecturas.stream().mapToLong(Lectura::getConsumoM3).sum();
        double promedioConsumo = lecturas.isEmpty() ? 0 : (double) totalM3 / lecturas.size();

        long asociadosActivos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.ACTIVO).size();
        long asociadosSuspendidos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.SUSPENDIDO).size();

        List<InformePeriodoResponse.FacturaResumenItem> facturaItems = facturas.stream()
                .sorted((a, b) -> b.getFechaEmision().compareTo(a.getFechaEmision()))
                .map(f -> InformePeriodoResponse.FacturaResumenItem.builder()
                        .numeroFactura(f.getNumeroFactura())
                        .asociadoNombre(f.getAsociado().getNombres() + " " + f.getAsociado().getApellidos())
                        .fechaEmision(f.getFechaEmision())
                        .total(f.getTotal())
                        .estado(f.getEstado().name())
                        .build())
                .toList();

        List<InformePeriodoResponse.MovimientoResumenItem> movimientoItems = movimientosVigentes.stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(m -> InformePeriodoResponse.MovimientoResumenItem.builder()
                        .numero((m.getTipo() == TipoMovimiento.ENTRADA ? "ENT-" : "SAL-") + String.format("%06d", m.getNumero()))
                        .tipo(m.getTipo().name())
                        .concepto(m.getConcepto())
                        .valor(m.getValor())
                        .fecha(m.getFecha().toLocalDate())
                        .build())
                .toList();

        return InformePeriodoResponse.builder()
                .tipoInforme(tipoInforme)
                .tituloPeriodo(titulo)
                .fechaGeneracion(LocalDate.now())
                .totalIngresos(totalIngresos)
                .totalGastos(totalGastos)
                .balance(totalIngresos.subtract(totalGastos))
                .facturasGeneradas(facturas.size())
                .facturasPagadas(facturasPagadas)
                .facturasPendientes(facturasPendientes)
                .facturasVencidas(facturasVencidas)
                .facturasAnuladas(facturasAnuladas)
                .totalFacturado(totalFacturado)
                .totalRecaudadoFacturas(totalRecaudadoFacturas)
                .numeroMultas(numeroMultas)
                .totalMultas(totalMultas)
                .totalM3Consumidos(totalM3)
                .promedioConsumoM3(promedioConsumo)
                .asociadosActivos(asociadosActivos)
                .asociadosSuspendidos(asociadosSuspendidos)
                .facturas(facturaItems)
                .movimientos(movimientoItems)
                .build();
    }

    // ================= INFORME DE SEGUIMIENTO A UN ASOCIADO =================

    public InformeAsociadoResponse generarInformeAsociado(Long asociadoId) {
        Asociado asociado = asociadoRepository.findById(asociadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"));

        List<Factura> facturas = facturaRepository.findByAsociadoId(asociadoId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Pago> pagos = pagoRepository.findByAsociadoId(asociadoId);
        List<Multa> multas = multaRepository.findByAsociadoId(asociadoId);

        long facturasPagadas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PAGADA).count();
        long facturasPendientes = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PENDIENTE || f.getEstado() == EstadoFactura.PAGADA_PARCIAL).count();
        long facturasVencidas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.VENCIDA).count();

        BigDecimal totalFacturado = facturas.stream().map(Factura::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPagado = facturas.stream().map(Factura::getTotalPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPendiente = facturas.stream().map(Factura::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMultas = multas.stream().map(Multa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InformeAsociadoResponse.FacturaResumenItem> facturaItems = facturas.stream()
                .sorted((a, b) -> b.getFechaEmision().compareTo(a.getFechaEmision()))
                .map(f -> InformeAsociadoResponse.FacturaResumenItem.builder()
                        .numeroFactura(f.getNumeroFactura())
                        .fechaEmision(f.getFechaEmision())
                        .total(f.getTotal())
                        .saldoPendiente(f.getSaldoPendiente())
                        .estado(f.getEstado().name())
                        .build())
                .toList();

        List<InformeAsociadoResponse.PagoResumenItem> pagoItems = pagos.stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(p -> InformeAsociadoResponse.PagoResumenItem.builder()
                        .numeroRecibo(reciboRepository.findByPagoId(p.getId()).map(Recibo::getNumeroRecibo).orElse("-"))
                        .numeroFactura(p.getFactura().getNumeroFactura())
                        .valor(p.getValor())
                        .fecha(p.getFecha())
                        .metodoPago(p.getMetodoPago().getNombre())
                        .build())
                .toList();

        List<InformeAsociadoResponse.MultaResumenItem> multaItems = multas.stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(m -> InformeAsociadoResponse.MultaResumenItem.builder()
                        .motivo(m.getMotivo())
                        .valor(m.getValor())
                        .fecha(m.getFecha())
                        .estado(m.getEstado().name())
                        .build())
                .toList();

        return InformeAsociadoResponse.builder()
                .fechaGeneracion(LocalDate.now())
                .codigoInterno(asociado.getCodigoInterno())
                .documento(asociado.getTipoDocumento() + " " + asociado.getDocumento())
                .nombreCompleto(asociado.getNombres() + " " + asociado.getApellidos())
                .telefonoPrincipal(asociado.getTelefonoPrincipal())
                .direccion(asociado.getDireccion())
                .estadoServicio(asociado.getEstadoServicio().name())
                .fechaAfiliacion(asociado.getFechaAfiliacion())
                .numeroMedidor(asociado.getMedidor() != null ? asociado.getMedidor().getNumero() : null)
                .totalFacturas(facturas.size())
                .facturasPagadas(facturasPagadas)
                .facturasPendientes(facturasPendientes)
                .facturasVencidas(facturasVencidas)
                .totalFacturado(totalFacturado)
                .totalPagado(totalPagado)
                .totalPendiente(totalPendiente)
                .numeroMultas(multas.size())
                .totalMultas(totalMultas)
                .facturas(facturaItems)
                .pagos(pagoItems)
                .multas(multaItems)
                .build();
    }
}
