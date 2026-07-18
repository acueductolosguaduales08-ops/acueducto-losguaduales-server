package com.acueducto.backend.service;

import com.acueducto.backend.dto.request.LecturaRequest;
import com.acueducto.backend.dto.response.LecturaResponse;
import com.acueducto.backend.entity.Lectura;
import com.acueducto.backend.entity.MesContable;
import com.acueducto.backend.entity.Medidor;
import com.acueducto.backend.entity.enums.EstadoMes;
import com.acueducto.backend.exception.RecursoDuplicadoException;
import com.acueducto.backend.exception.RecursoNoEncontradoException;
import com.acueducto.backend.exception.ReglaNegocioException;
import com.acueducto.backend.repository.LecturaRepository;
import com.acueducto.backend.repository.MesContableRepository;
import com.acueducto.backend.repository.MedidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Modulo de Medidores y Consumos (Modulo 6). El calculo del consumo siempre se hace en el
 * servidor (6.6 / 6.14) y nunca puede ser negativo.
 */
@Service
@RequiredArgsConstructor
public class LecturaService {

    private final LecturaRepository lecturaRepository;
    private final MedidorRepository medidorRepository;
    private final MesContableRepository mesContableRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public LecturaResponse registrar(LecturaRequest request) {
        Medidor medidor = medidorRepository.findById(request.medidorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Medidor no encontrado"));

        if (medidor.getAsociado() == null) {
            throw new ReglaNegocioException("El medidor no tiene un asociado asignado.");
        }

        MesContable mes = mesContableRepository.findById(request.mesContableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mes contable no encontrado"));

        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new ReglaNegocioException("El periodo esta cerrado. No es posible registrar lecturas.");
        }

        if (lecturaRepository.findByMedidorIdAndMesContableId(medidor.getId(), mes.getId()).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe una lectura registrada para este medidor en el periodo indicado.");
        }

        int lecturaAnterior = lecturaRepository.findTopByMedidorIdOrderByFechaLecturaDesc(medidor.getId())
                .map(Lectura::getLecturaActual)
                .orElse(0);

        if (request.lecturaActual() < lecturaAnterior) {
            throw new ReglaNegocioException(
                    "La lectura actual (" + request.lecturaActual() + ") no puede ser menor que la lectura anterior (" + lecturaAnterior + ").");
        }

        Lectura lectura = Lectura.builder()
                .asociado(medidor.getAsociado())
                .medidor(medidor)
                .mesContable(mes)
                .fechaLectura(request.fechaLectura())
                .lecturaAnterior(lecturaAnterior)
                .lecturaActual(request.lecturaActual())
                .observaciones(request.observaciones())
                .facturaGenerada(false)
                .anulada(false)
                .build();

        lectura = lecturaRepository.save(lectura);
        auditoriaService.registrar("REGISTRAR_LECTURA", "MEDIDORES", medidor.getNumero(),
                "Consumo: " + lectura.getConsumoM3() + " m3");
        return LecturaResponse.fromEntity(lectura);
    }

    @Transactional
    public LecturaResponse editar(Long id, LecturaRequest request) {
        Lectura lectura = obtenerEntidad(id);
        if (lectura.isFacturaGenerada()) {
            throw new ReglaNegocioException("No se puede editar una lectura que ya genero una factura. Anule y regenere la factura.");
        }
        if (request.lecturaActual() < lectura.getLecturaAnterior()) {
            throw new ReglaNegocioException("La lectura actual no puede ser menor que la lectura anterior.");
        }
        lectura.setLecturaActual(request.lecturaActual());
        lectura.setFechaLectura(request.fechaLectura());
        lectura.setObservaciones(request.observaciones());
        lectura = lecturaRepository.save(lectura);
        auditoriaService.registrar("EDITAR_LECTURA", "MEDIDORES", lectura.getMedidor().getNumero(), null);
        return LecturaResponse.fromEntity(lectura);
    }

    public LecturaResponse obtener(Long id) {
        return LecturaResponse.fromEntity(obtenerEntidad(id));
    }

    public Lectura obtenerEntidad(Long id) {
        return lecturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lectura no encontrada con id " + id));
    }

    public List<LecturaResponse> historialPorAsociado(Long asociadoId) {
        return lecturaRepository.findByAsociadoIdOrderByFechaLecturaDesc(asociadoId).stream()
                .map(LecturaResponse::fromEntity).toList();
    }

    public List<LecturaResponse> porMes(Long mesContableId) {
        return lecturaRepository.findByMesContableId(mesContableId).stream()
                .map(LecturaResponse::fromEntity).toList();
    }
}
