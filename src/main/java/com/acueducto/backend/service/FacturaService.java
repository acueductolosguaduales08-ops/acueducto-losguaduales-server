package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.ConceptoFacturaRequest;
import com.acueducto.backend.dto.response.FacturaResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoMes;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import com.acueducto.backend.util.NumeracionUtil;
import com.acueducto.backend.util.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulo de Facturacion (Modulo 7). Genera facturas a partir de las lecturas confirmadas
 * del mes (flujo recomendado en 6.14: registrar todas las lecturas -> "Generar Facturacion del Mes"),
 * garantizando que todos los calculos se realicen en el servidor (4.2 / 6.14 / 7.13).
 */
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final LecturaRepository lecturaRepository;
    private final MesContableRepository mesContableRepository;
    private final MultaRepository multaRepository;
    private final ConfiguracionService configuracionService;
    private final QrCodeService qrCodeService;
    private final ConceptoFacturaRepository conceptoFacturaRepository;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;

    /**
     * Ejecuta el proceso masivo "Generar Facturacion del Mes": recorre todas las lecturas
     * pendientes del periodo y genera una factura por cada una, evitando facturas incompletas.
     */
    @Transactional
    public List<FacturaResponse> generarFacturacionMes(Long mesContableId) {
        MesContable mes = mesContableRepository.findById(mesContableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mes contable no encontrado"));

        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new ReglaNegocioException("El periodo esta cerrado. No es posible generar facturas.");
        }

        List<Lectura> lecturasPendientes = lecturaRepository.findByMesContableIdAndFacturaGeneradaFalse(mesContableId);
        if (lecturasPendientes.isEmpty()) {
            throw new ReglaNegocioException("No hay lecturas pendientes de facturar en este periodo.");
        }

        List<FacturaResponse> generadas = new ArrayList<>();
        for (Lectura lectura : lecturasPendientes) {
            generadas.add(generarFacturaDesdeLectura(lectura, mes));
        }

        auditoriaService.registrar("GENERAR_FACTURACION_MES", "FACTURACION",
                mes.getNombreMes() + " " + mes.getAnioContable().getAnio(),
                generadas.size() + " factura(s) generadas");

        return generadas;
    }

    private FacturaResponse generarFacturaDesdeLectura(Lectura lectura, MesContable mes) {
        if (facturaRepository.existsByAsociadoIdAndMesContableId(lectura.getAsociado().getId(), mes.getId())) {
            throw new ReglaNegocioException(
                    "El asociado " + lectura.getAsociado().getDocumento() + " ya tiene una factura generada para este periodo.");
        }

        Configuracion config = configuracionService.obtenerEntidad();

        int consumo = lectura.getConsumoM3();
        BigDecimal valorConsumo = config.getValorM3().multiply(BigDecimal.valueOf(consumo)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cargoAdministracion = config.getCargoFijoAdministracion();

        // Multas pendientes del asociado se incluyen automaticamente en la nueva factura (6.8)
        List<Multa> multasPendientes = multaRepository.findByAsociadoIdAndEstado(
                lectura.getAsociado().getId(), com.acueducto.backend.entity.enums.EstadoMulta.PENDIENTE);
        BigDecimal totalMultas = multasPendientes.stream().map(Multa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = valorConsumo.add(cargoAdministracion).add(totalMultas);

        long consecutivo = configuracionService.siguienteNumeroFactura();
        String numeroFactura = NumeracionUtil.formatearFactura(consecutivo);

        Factura factura = Factura.builder()
                .numeroFactura(numeroFactura)
                .asociado(lectura.getAsociado())
                .lectura(lectura)
                .mesContable(mes)
                .fechaEmision(LocalDate.now())
                .fechaLimitePago(LocalDate.now().plusDays(config.getDiasPlazoPago()))
                .lecturaAnterior(lectura.getLecturaAnterior())
                .lecturaActual(lectura.getLecturaActual())
                .consumoM3(consumo)
                .valorM3(config.getValorM3())
                .valorConsumo(valorConsumo)
                .cargoAdministracion(cargoAdministracion)
                .valoresAdicionales(BigDecimal.ZERO)
                .totalMultas(totalMultas)
                .total(total)
                .totalPagado(BigDecimal.ZERO)
                .estado(EstadoFactura.PENDIENTE)
                .build();

        factura = facturaRepository.save(factura);
        factura.setCodigoQr(qrCodeService.generarQrFactura(factura.getNumeroFactura()));
        factura = facturaRepository.save(factura);

        lectura.setFacturaGenerada(true);
        lecturaRepository.save(lectura);

        for (Multa multa : multasPendientes) {
            multa.setFactura(factura);
        }
        multaRepository.saveAll(multasPendientes);

        notificacionService.notificarFacturaGenerada(factura);

        return FacturaResponse.fromEntity(factura);
    }

    @Transactional
    public FacturaResponse agregarConcepto(ConceptoFacturaRequest request) {
        Factura factura = obtenerEntidad(request.facturaId());
        if (factura.getEstado() == EstadoFactura.PAGADA || factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("No se puede modificar una factura pagada o anulada.");
        }

        ConceptoFactura concepto = ConceptoFactura.builder()
                .factura(factura).descripcion(request.descripcion()).valor(request.valor())
                .build();
        conceptoFacturaRepository.save(concepto);

        factura.setValoresAdicionales(factura.getValoresAdicionales().add(request.valor()));
        factura.setTotal(factura.getTotal().add(request.valor()));
        factura = facturaRepository.save(factura);

        auditoriaService.registrar("AGREGAR_CONCEPTO_FACTURA", "FACTURACION", factura.getNumeroFactura(), request.descripcion());
        return FacturaResponse.fromEntity(factura);
    }

    @Transactional
    public FacturaResponse anular(Long id, String motivo) {
        Factura factura = obtenerEntidad(id);
        if (factura.getEstado() == EstadoFactura.PAGADA) {
            throw new ReglaNegocioException("No es posible anular una factura ya pagada.");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        factura.setMotivoAnulacion(motivo);
        factura = facturaRepository.save(factura);
        auditoriaService.registrar("ANULAR_FACTURA", "FACTURACION", factura.getNumeroFactura(), motivo);
        return FacturaResponse.fromEntity(factura);
    }

    /** Marca automaticamente como vencidas las facturas pendientes cuya fecha limite ya paso (2.11 / 7.5). */
    @Transactional
    public int marcarFacturasVencidas() {
        List<Factura> pendientes = facturaRepository.findByEstadoAndFechaLimitePagoBefore(EstadoFactura.PENDIENTE, LocalDate.now());
        pendientes.forEach(f -> f.setEstado(EstadoFactura.VENCIDA));
        facturaRepository.saveAll(pendientes);
        return pendientes.size();
    }

    public FacturaResponse obtener(Long id) {
        return FacturaResponse.fromEntity(obtenerEntidad(id));
    }

    public Factura obtenerEntidad(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada con id " + id));
    }

    public Factura obtenerPorNumero(String numeroFactura) {
        return facturaRepository.findByNumeroFactura(numeroFactura)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + numeroFactura));
    }

    public Page<FacturaResponse> listarPorAsociado(Long asociadoId, Pageable pageable) {
        return facturaRepository.findByAsociadoId(asociadoId, pageable).map(FacturaResponse::fromEntity);
    }

    public Page<FacturaResponse> listarPorEstado(EstadoFactura estado, Pageable pageable) {
        return facturaRepository.findByEstado(estado, pageable).map(FacturaResponse::fromEntity);
    }

    /** Historial completo de facturas, sin filtrar por asociado ni estado (util para Swagger y para el panel de Tesoreria/Administrador). */
    public Page<FacturaResponse> listarTodas(Pageable pageable) {
        return facturaRepository.findAll(pageable).map(FacturaResponse::fromEntity);
    }
}
