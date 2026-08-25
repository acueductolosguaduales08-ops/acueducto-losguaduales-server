package com.acueducto.backend.service;

import com.acueducto.backend.dto.response.MovimientoBancarioResponse;
import com.acueducto.backend.entity.MovimientoBancario;
import com.acueducto.backend.entity.MovimientoTesoreria;
import com.acueducto.backend.entity.enums.EstadoConciliacion;
import com.acueducto.backend.entity.enums.TipoMovimiento;
import com.acueducto.backend.repository.MovimientoBancarioRepository;
import com.acueducto.backend.repository.MovimientoTesoreriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConciliacionService {

    private final MovimientoBancarioRepository movimientoBancarioRepository;
    private final MovimientoTesoreriaRepository movimientoTesoreriaRepository;
    private final AuditoriaService auditoriaService;

    public Map<String, Object> importarExtracto(List<String[]> filas, String nombreArchivo) {
        int importados = 0;
        int errores = 0;
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        };

        for (String[] fila : filas) {
            try {
                if (fila.length < 3) { errores++; continue; }

                LocalDate fecha = null;
                for (DateTimeFormatter fmt : formatters) {
                    try {
                        fecha = LocalDate.parse(fila[0].trim(), fmt);
                        break;
                    } catch (Exception ignored) {}
                }
                if (fecha == null) { errores++; continue; }

                String descripcion = fila.length > 1 ? fila[1].trim() : "Sin descripcion";
                BigDecimal valor = new BigDecimal(fila[2].trim().replace(",", "").replace(".", ""));
                String referencia = fila.length > 3 ? fila[3].trim() : null;

                MovimientoBancario mb = MovimientoBancario.builder()
                        .fechaTransaccion(fecha)
                        .descripcion(descripcion)
                        .valor(valor.abs())
                        .numeroReferencia(referencia)
                        .estado(EstadoConciliacion.PENDIENTE)
                        .archivoOrigen(nombreArchivo)
                        .build();
                movimientoBancarioRepository.save(mb);
                importados++;
            } catch (Exception e) {
                errores++;
            }
        }

        auditoriaService.registrar("IMPORTAR_EXTRACTO_BANCARIO", "CONCILIACION",
                nombreArchivo, importados + " registros importados, " + errores + " errores");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("importados", importados);
        resultado.put("errores", errores);
        resultado.put("total", importados + errores);
        return resultado;
    }

    public List<MovimientoBancarioResponse> listarPendientes() {
        return movimientoBancarioRepository.findByEstadoOrderByFechaTransaccionDesc(EstadoConciliacion.PENDIENTE)
                .stream().map(MovimientoBancarioResponse::fromEntity).toList();
    }

    public List<MovimientoBancarioResponse> listarTodos() {
        return movimientoBancarioRepository.findAllByOrderByFechaTransaccionDesc()
                .stream().map(MovimientoBancarioResponse::fromEntity).toList();
    }

    @Transactional
    public MovimientoBancarioResponse conciliar(Long bancarioId, Long tesoreriaId) {
        MovimientoBancario mb = movimientoBancarioRepository.findById(bancarioId)
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException("Movimiento bancario no encontrado"));
        MovimientoTesoreria mt = movimientoTesoreriaRepository.findById(tesoreriaId)
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException("Movimiento de tesoreria no encontrado"));

        mb.setEstado(EstadoConciliacion.CONCILIADO);
        mb.setMovimientoTesoreria(mt);
        mb = movimientoBancarioRepository.save(mb);

        auditoriaService.registrar("CONCILIAR_MOVIMIENTO", "CONCILIACION",
                mb.getNumeroReferencia(), "Bancario #" + bancarioId + " -> Tesoreria #" + tesoreriaId);

        return MovimientoBancarioResponse.fromEntity(mb);
    }

    @Transactional
    public MovimientoBancarioResponse marcarSinCoincidencia(Long bancarioId) {
        MovimientoBancario mb = movimientoBancarioRepository.findById(bancarioId)
                .orElseThrow(() -> new com.acueducto.backend.exception.RecursoNoEncontradoException("Movimiento bancario no encontrado"));

        mb.setEstado(EstadoConciliacion.SIN_COINCIDENCIA);
        mb = movimientoBancarioRepository.save(mb);

        auditoriaService.registrar("MARCAR_SIN_COINCIDENCIA", "CONCILIACION",
                mb.getNumeroReferencia(), "Movimiento bancario #" + bancarioId);

        return MovimientoBancarioResponse.fromEntity(mb);
    }

    public Map<String, Object> obtenerResumen() {
        long pendientes = movimientoBancarioRepository.countByEstado(EstadoConciliacion.PENDIENTE);
        long conciliados = movimientoBancarioRepository.countByEstado(EstadoConciliacion.CONCILIADO);
        long sinCoincidencia = movimientoBancarioRepository.countByEstado(EstadoConciliacion.SIN_COINCIDENCIA);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("pendientes", pendientes);
        resumen.put("conciliados", conciliados);
        resumen.put("sinCoincidencia", sinCoincidencia);
        resumen.put("total", pendientes + conciliados + sinCoincidencia);
        return resumen;
    }
}
