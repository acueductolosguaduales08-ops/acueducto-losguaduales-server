package com.acueducto.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Repara en cada arranque columnas {@code NOT NULL} que Hibernate NO pudo agregar con
 * {@code spring.jpa.hibernate.ddl-auto=update} a tablas que ya tenian filas en produccion.
 *
 * <p>Problema que resuelve: {@code ddl-auto=update} agrega columnas nuevas a una tabla vacia,
 * pero NO puede agregar una columna {@code NOT NULL} a una tabla que ya tiene filas (la base la
 * rechaza sin un valor por defecto). Asi, columnas agregadas en rondas posteriores quedaban
 * ausentes en la base de produccion y cualquier SELECT que las incluyera fallaba con 500
 * ("column ... does not exist"). Ejemplos: {@code configuracion.modo_hero},
 * {@code configuracion.auditoria_activa}, {@code multa.independiente}.
 *
 * <p>Este migrador corre DESPUES de que Hibernate haya creado/actualizado el esquema (por eso se
 * usa {@link ApplicationRunner}), asi que las tablas ya existen. Para cada columna declarada
 * ejecuta ALTERs idempotentes ({@code ADD COLUMN IF NOT EXISTS}), rellena las filas existentes
 * con el valor por defecto de la entidad y recien entonces aplica {@code NOT NULL}. Es seguro
 * ejecutarlo en cada arranque y funciona tanto en H2 (dev) como en PostgreSQL (prod).
 */
@Component
@Order(1)
public class ConfiguracionSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSchemaMigrator.class);

    /** (tabla, columna, tipo SQL, valor por defecto con el que se rellenan las filas existentes). */
    private static final List<ColumnaReparar> COLUMNAS = List.of(
            new ColumnaReparar("configuracion", "modo_hero", "VARCHAR(20)", "'UNICO'"),
            new ColumnaReparar("configuracion", "hero_rotacion_actual_id", "BIGINT", null),
            new ColumnaReparar("configuracion", "hero_rotacion_desde", "TIMESTAMP", null),
            new ColumnaReparar("configuracion", "auditoria_activa", "BOOLEAN", "TRUE"),
            new ColumnaReparar("configuracion", "edicion_asociados_activa", "BOOLEAN", "TRUE"),
            new ColumnaReparar("multas", "independiente", "BOOLEAN", "FALSE")
    );

    private final JdbcTemplate jdbcTemplate;

    public ConfiguracionSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        renombrarColumnaEmailAContacto();
        for (ColumnaReparar columna : COLUMNAS) {
            if (!existeTabla(columna.tabla())) {
                continue;
            }
            reparar(columna);
        }
        log.info("Esquema verificado: columnas NOT NULL faltantes reparadas si hacia falta.");
    }

    private void renombrarColumnaEmailAContacto() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) = 'usuarios' AND LOWER(column_name) = 'email'",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE usuarios RENAME COLUMN email TO contacto");
                jdbcTemplate.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS uk_usuario_email");
                jdbcTemplate.execute("ALTER TABLE usuarios ADD CONSTRAINT uk_usuario_contacto UNIQUE (contacto)");
                log.info("Columna 'email' renombrada a 'contacto' en tabla usuarios.");
            }
        } catch (Exception e) {
            log.warn("No se pudo renombrar email a contacto: {}", e.getMessage());
        }
    }

    private void reparar(ColumnaReparar columna) {
        try {
            // 1) Agregar la columna si no existe. Se agrega sin NOT NULL de momento, porque la
            //    tabla ya tiene filas y no se puede poner NOT NULL sin un valor por defecto.
            jdbcTemplate.execute("ALTER TABLE " + columna.tabla()
                    + " ADD COLUMN IF NOT EXISTS " + columna.columna() + " " + columna.tipo());

            // 2) Rellenar las filas existentes con el valor por defecto que define la entidad.
            if (columna.defecto() != null) {
                jdbcTemplate.update("UPDATE " + columna.tabla()
                        + " SET " + columna.columna() + " = " + columna.defecto()
                        + " WHERE " + columna.columna() + " IS NULL");
            }

            // 3) Solo las columnas con valor por defecto (NOT NULL en la entidad) se marcan
            //    NOT NULL, una vez que ya no hay NULLs. Las demas (ej. hero_rotacion_*) quedan
            //    nullable, tal como las declara la entidad.
            if (columna.defecto() != null) {
                jdbcTemplate.execute("ALTER TABLE " + columna.tabla()
                        + " ALTER COLUMN " + columna.columna() + " SET NOT NULL");
            }
        } catch (Exception e) {
            log.warn("No se pudo reparar {}.{}: {}", columna.tabla(), columna.columna(), e.getMessage());
        }
    }

    private boolean existeTabla(String tabla) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = LOWER(?)",
                    Integer.class, tabla);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private record ColumnaReparar(String tabla, String columna, String tipo, String defecto) {
    }
}
