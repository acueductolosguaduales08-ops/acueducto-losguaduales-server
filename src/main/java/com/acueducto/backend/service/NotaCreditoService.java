package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.NotaCreditoRequest;
import com.acueducto.backend.dto.response.NotaCreditoResponse;
import com.acueducto.backend.entity.*;
import com.acueducto.backend.entity.enums.EstadoFactura;
import com.acueducto.backend.entity.enums.EstadoNotaCredito;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.*;
import com.acueducto.backend.util.NumeracionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotaCreditoService {

    private final NotaCreditoRepository notaCreditoRepository;
    private final AsociadoRepository asociadoRepository;
    private final FacturaRepository facturaRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public NotaCreditoResponse crear(NotaCreditoRequest request) {
        Asociado asociado = asociadoRepository.findById(request.asociadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Asociado no encontrado"));

        Factura factura = null;
        if (request.facturaId() != null) {
            factura = facturaRepository.findById(request.facturaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
            if (factura.getAsociado().getId().equals(asociado.getId()) == false) {
                throw new ReglaNegocioException("La factura no pertenece al asociado indicado");
            }
        }

        Configuracion config = configuracionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuracion no encontrada"));

        String numeroNota = NumeracionUtil.formatearNotaCredito(config.getSiguienteNumeroNotaCredito());
        config.setSiguienteNumeroNotaCredito(config.getSiguienteNumeroNotaCredito() + 1);
        configuracionRepository.save(config);

        NotaCredito nc = NotaCredito.builder()
                .numeroNota(numeroNota)
                .asociado(asociado)
                .factura(factura)
                .motivo(request.motivo())
                .valor(request.valor())
                .fechaEmision(LocalDate.now())
                .estado(EstadoNotaCredito.PENDIENTE)
                .observaciones(request.observaciones())
                .build();

        nc = notaCreditoRepository.save(nc);

        auditoriaService.registrar("CREAR_NOTA_CREDITO", "NOTAS_CREDITO",
                numeroNota, "Nota de credito por $" + request.valor() + " - " + request.motivo());

        return NotaCreditoResponse.fromEntity(nc);
    }

    @Transactional
    public NotaCreditoResponse aplicar(Long notaCreditoId) {
        NotaCredito nc = notaCreditoRepository.findById(notaCreditoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nota de credito no encontrada"));

        if (nc.getEstado() != EstadoNotaCredito.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden aplicar notas de credito en estado PENDIENTE");
        }

        if (nc.getFactura() == null) {
            throw new ReglaNegocioException("Esta nota de credito no esta asociada a una factura");
        }

        Factura factura = nc.getFactura();
        BigDecimal nuevoPagado = factura.getTotalPagado().add(nc.getValor());
        if (nuevoPagado.compareTo(factura.getTotal()) > 0) {
            throw new ReglaNegocioException("El valor de la nota de credito excede el saldo pendiente de la factura");
        }

        factura.setTotalPagado(nuevoPagado);
        if (factura.getTotalPagado().compareTo(factura.getTotal()) >= 0) {
            factura.setEstado(EstadoFactura.PAGADA);
        } else {
            factura.setEstado(EstadoFactura.PAGADA_PARCIAL);
        }
        facturaRepository.save(factura);

        nc.setEstado(EstadoNotaCredito.APLICADA);
        nc = notaCreditoRepository.save(nc);

        auditoriaService.registrar("APLICAR_NOTA_CREDITO", "NOTAS_CREDITO",
                nc.getNumeroNota(), "Aplicada a factura " + factura.getNumeroFactura() + " - $" + nc.getValor());

        return NotaCreditoResponse.fromEntity(nc);
    }

    @Transactional
    public NotaCreditoResponse anular(Long notaCreditoId, String motivoAnulacion) {
        NotaCredito nc = notaCreditoRepository.findById(notaCreditoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nota de credito no encontrada"));

        if (nc.getEstado() == EstadoNotaCredito.ANULADA) {
            throw new ReglaNegocioException("La nota de credito ya esta anulada");
        }

        if (nc.getEstado() == EstadoNotaCredito.APLICADA) {
            throw new ReglaNegocioException("No se puede anular una nota de credito ya aplicada");
        }

        nc.setEstado(EstadoNotaCredito.ANULADA);
        nc.setObservaciones(motivoAnulacion);
        nc = notaCreditoRepository.save(nc);

        auditoriaService.registrar("ANULAR_NOTA_CREDITO", "NOTAS_CREDITO",
                nc.getNumeroNota(), "Anulada: " + motivoAnulacion);

        return NotaCreditoResponse.fromEntity(nc);
    }

    public Page<NotaCreditoResponse> listar(Pageable pageable) {
        return notaCreditoRepository.findAll(pageable).map(NotaCreditoResponse::fromEntity);
    }

    public Page<NotaCreditoResponse> listarPorAsociado(Long asociadoId, Pageable pageable) {
        return notaCreditoRepository.findByAsociadoId(asociadoId, pageable).map(NotaCreditoResponse::fromEntity);
    }

    public NotaCreditoResponse obtenerPorId(Long id) {
        NotaCredito nc = notaCreditoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nota de credito no encontrada"));
        return NotaCreditoResponse.fromEntity(nc);
    }
}
