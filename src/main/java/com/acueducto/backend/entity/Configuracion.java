package com.acueducto.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Parametros generales del sistema (Modulo 10). Se maneja como una fila unica (singleton)
 * que centraliza datos institucionales, tarifas, informacion bancaria y numeracion.
 */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion extends BaseEntity {

    // ---- Informacion institucional ----
    @Column(name = "nombre_acueducto", nullable = false, length = 150)
    private String nombreAcueducto;

    @Column(length = 30)
    private String nit;

    @Column(length = 200)
    private String direccion;

    @Column(name = "telefono_principal", length = 20)
    private String telefonoPrincipal;

    @Column(length = 150)
    private String correo;

    @Column(length = 100)
    private String municipio;

    @Column(length = 100)
    private String departamento;

    // ---- Datos bancarios ----
    @Column(length = 100)
    private String banco;

    @Column(name = "tipo_cuenta", length = 30)
    private String tipoCuenta;

    @Column(name = "numero_cuenta", length = 40)
    private String numeroCuenta;

    @Column(name = "titular_cuenta", length = 150)
    private String titularCuenta;

    // ---- Tarifas vigentes ----
    @Column(name = "valor_m3", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorM3;

    @Column(name = "cargo_fijo_administracion", nullable = false, precision = 12, scale = 2)
    private BigDecimal cargoFijoAdministracion;

    @Column(name = "valor_reconexion", precision = 12, scale = 2)
    private BigDecimal valorReconexion;

    @Column(name = "valor_multa_defecto", precision = 12, scale = 2)
    private BigDecimal valorMultaDefecto;

    // ---- Parametros generales ----
    @Builder.Default
    @Column(name = "dias_plazo_pago", nullable = false)
    private Integer diasPlazoPago = 15;

    @Builder.Default
    @Column(name = "siguiente_numero_factura", nullable = false)
    private Long siguienteNumeroFactura = 1L;

    @Builder.Default
    @Column(name = "siguiente_numero_recibo", nullable = false)
    private Long siguienteNumeroRecibo = 1L;

    @Builder.Default
    @Column(name = "siguiente_numero_entrada", nullable = false)
    private Long siguienteNumeroEntrada = 1L;

    @Builder.Default
    @Column(name = "siguiente_numero_salida", nullable = false)
    private Long siguienteNumeroSalida = 1L;

    @Column(name = "notas_factura", columnDefinition = "TEXT")
    private String notasFactura;

    // ---- Archivos institucionales activos (rutas relativas, ver ArchivoInstitucional) ----
    // TEXT en vez de un largo fijo: antes (length = 300) una URL remota larga rompia el guardado
    // con "Los datos enviados no se pudieron guardar...". Ahora ademas el backend siempre descarga
    // y guarda copia local de logo/firma/sello, por lo que aqui solo se almacenan rutas cortas,
    // pero se deja sin limite por robustez.
    @Column(name = "logo_activo", columnDefinition = "TEXT")
    private String logoActivo;

    @Column(name = "firma_activa", columnDefinition = "TEXT")
    private String firmaActiva;

    @Column(name = "sello_activo", columnDefinition = "TEXT")
    private String selloActivo;

    /** Interruptor general de auditoria (solo el Administrador puede desactivarlo). */
    @Builder.Default
    @Column(name = "auditoria_activa", nullable = false)
    private boolean auditoriaActiva = true;
}
