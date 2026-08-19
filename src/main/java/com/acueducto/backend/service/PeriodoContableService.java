package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.AnioContableRequest;
import com.acueducto.backend.dto.request.MesContableRequest;
import com.acueducto.backend.dto.response.AnioContableResponse;
import com.acueducto.backend.dto.response.MesContableResponse;
import com.acueducto.backend.dto.response.ResumenPeriodoResponse;
import com.acueducto.backend.entity.AnioContable;
import com.acueducto.backend.entity.Lectura;
import com.acueducto.backend.entity.MesContable;
import com.acueducto.backend.entity.enums.EstadoAnio;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoMes;
import com.acueducto.backend.entity.enums.EstadoServicio;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.AnioContableRepository;
import com.acueducto.backend.repository.AsociadoRepository;
import com.acueducto.backend.repository.FacturaRepository;
import com.acueducto.backend.repository.LecturaRepository;
import com.acueducto.backend.repository.MesContableRepository;
import com.acueducto.backend.repository.MovimientoTesoreriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Modulo de Periodos Contables (Modulo 9). Organiza cronologicamente toda la informacion
 * financiera y administrativa; solo puede existir un periodo abierto para registrar operaciones.
 */
@Service
@RequiredArgsConstructor
public class PeriodoContableService {

    private final AnioContableRepository anioContableRepository;
    private final MesContableRepository mesContableRepository;
    private final LecturaRepository lecturaRepository;
    private final FacturaRepository facturaRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final AsociadoRepository asociadoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AnioContableResponse crearAnio(AnioContableRequest request) {
        if (anioContableRepository.existsByAnio(request.anio())) {
            throw new RecursoDuplicadoException("El anio " + request.anio() + " ya existe.");
        }
        AnioContable anio = AnioContable.builder().anio(request.anio()).estado(EstadoAnio.ACTIVO).build();
        anio = anioContableRepository.save(anio);
        auditoriaService.registrar("CREAR_ANIO", "PERIODOS_CONTABLES", String.valueOf(anio.getAnio()), null);
        return AnioContableResponse.fromEntity(anio);
    }

    @Transactional
    public MesContableResponse crearMes(MesContableRequest request) {
        AnioContable anio = anioContableRepository.findById(request.anioContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Anio contable no encontrado"));

        if (mesContableRepository.findByNumeroMesAndAnioContableId(request.numeroMes(), anio.getId()).isPresent()) {
            throw new RecursoDuplicadoException("El mes indicado ya existe para este anio.");
        }

        String nombreMes = LocalDate.of(anio.getAnio(), request.numeroMes(), 1)
                .getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));

        MesContable mes = MesContable.builder()
                .nombreMes(nombreMes)
                .numeroMes(request.numeroMes())
                .anioContable(anio)
                .estado(EstadoMes.ABIERTO)
                .fechaApertura(LocalDate.now())
                .build();

        mes = mesContableRepository.save(mes);
        auditoriaService.registrar("CREAR_MES", "PERIODOS_CONTABLES", mes.getNombreMes() + " " + anio.getAnio(), null);
        return MesContableResponse.fromEntity(mes);
    }

    /** Verifica prerequisitos y cierra el periodo, bloqueando nuevas operaciones (9.8). */
    @Transactional
    public MesContableResponse cerrarMes(Long mesId) {
        MesContable mes = obtenerMesEntidad(mesId);

        List<Lectura> lecturasSinFactura = lecturaRepository.findByMesContableIdAndFacturaGeneradaFalse(mesId);
        if (!lecturasSinFactura.isEmpty()) {
            throw new ReglaNegocioException(
                    "No es posible cerrar el periodo: existen " + lecturasSinFactura.size()
                            + " lectura(s) sin factura generada. Ejecute la generacion de facturacion del mes primero.");
        }

        mes.setEstado(EstadoMes.CERRADO);
        mes.setFechaCierre(LocalDate.now());
        mes = mesContableRepository.save(mes);
        auditoriaService.registrar("CERRAR_MES", "PERIODOS_CONTABLES", mes.getNombreMes() + " " + mes.getAnioContable().getAnio(), null);
        return MesContableResponse.fromEntity(mes);
    }

    /** Reapertura excepcional, solo con autorizacion del Administrador (9.6). */
    @Transactional
    public MesContableResponse reabrirMes(Long mesId, String motivo) {
        MesContable mes = obtenerMesEntidad(mesId);
        mes.setEstado(EstadoMes.REABIERTO);
        mes = mesContableRepository.save(mes);
        auditoriaService.registrar("REABRIR_MES", "PERIODOS_CONTABLES",
                mes.getNombreMes() + " " + mes.getAnioContable().getAnio(), motivo);
        return MesContableResponse.fromEntity(mes);
    }

    public List<AnioContableResponse> listarAnios() {
        return anioContableRepository.findAll().stream().map(AnioContableResponse::fromEntity).toList();
    }

    public List<MesContableResponse> listarMesesPorAnio(Long anioId) {
        return mesContableRepository.findByAnioContableIdOrderByNumeroMes(anioId).stream()
                .map(MesContableResponse::fromEntity).toList();
    }

    public MesContable obtenerMesEntidad(Long id) {
        return mesContableRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mes contable no encontrado con id " + id));
    }

    /** Resumen automatico del periodo (9.9): facturacion, tesoreria, consumo y asociados. */
    public ResumenPeriodoResponse resumenMes(Long mesId) {
        MesContable mes = obtenerMesEntidad(mesId);
        var facturas = facturaRepository.findByMesContableId(mesId);

        long generadas = facturas.size();
        long pagadas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PAGADA).count();
        long pendientes = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.PENDIENTE
                || f.getEstado() == EstadoFactura.PAGADA_PARCIAL).count();
        long vencidas = facturas.stream().filter(f -> f.getEstado() == EstadoFactura.VENCIDA).count();

        var movimientos = movimientoTesoreriaRepository.findByMesContableId(mesId);
        BigDecimal ingresos = movimientos.stream().filter(m -> m.getTipo() == TipoMovimiento.ENTRADA && !m.isAnulado())
                .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gastos = movimientos.stream().filter(m -> m.getTipo() == TipoMovimiento.SALIDA && !m.isAnulado())
                .map(com.acueducto.backend.entity.MovimientoTesoreria::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        var lecturas = lecturaRepository.findByMesContableId(mesId);
        long totalM3 = lecturas.stream().mapToLong(Lectura::getConsumoM3).sum();
        double promedio = lecturas.isEmpty() ? 0 : (double) totalM3 / lecturas.size();

        long activos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.ACTIVO).size();
        long suspendidos = asociadoRepository.findByEstadoServicioAndArchivadoFalse(EstadoServicio.SUSPENDIDO).size();

        return ResumenPeriodoResponse.builder()
                .facturasGeneradas(generadas)
                .facturasPagadas(pagadas)
                .facturasPendientes(pendientes)
                .facturasVencidas(vencidas)
                .totalIngresos(ingresos)
                .totalGastos(gastos)
                .balance(ingresos.subtract(gastos))
                .totalM3Consumidos(totalM3)
                .promedioConsumo(promedio)
                .asociadosActivos(activos)
                .asociadosSuspendidos(suspendidos)
                .build();
    }
}
