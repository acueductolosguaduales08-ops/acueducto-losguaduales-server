package com.acueducto.backend.entity;

import com.acueducto.backend.entity.enums.TipoDocumentoPublico;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Enlace publico temporal para descargar la factura o el recibo en PDF sin iniciar sesion.
 * El token es aleatorio y de un solo uso efectivo: generar un enlace nuevo para el mismo
 * documento elimina el anterior, y los enlaces vencidos (72 horas por defecto) se borran
 * definitivamente. Es el enlace que se envia por WhatsApp para cobrar facturas o entregar
 * recibos (factura/recibo compartido).
 */
@Entity
@Table(name = "enlaces_publicos_documentos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enlace_publico_token", columnNames = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnlacePublicoDocumento extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoPublico tipoDocumento;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}