package com.acueducto.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica que el migrador de esquema agregue columnas NOT NULL a tablas que ya tenian filas
 * (el escenario exacto de produccion donde Hibernate con ddl-auto=update no puede hacerlo solo).
 */
class ConfiguracionSchemaMigratorTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private ConfiguracionSchemaMigrator migrador;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbc = new JdbcTemplate(dataSource);
        migrador = new ConfiguracionSchemaMigrator(jdbc);

        // Simula la tabla configuracion ANTES de la ronda que agrego modo_hero: sin esa columna.
        jdbc.execute("CREATE TABLE configuracion (id BIGINT PRIMARY KEY, nombre_acueducto VARCHAR(150))");
        jdbc.execute("INSERT INTO configuracion (id, nombre_acueducto) VALUES (1, 'Acueducto')");
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("DROP ALL OBJECTS");
    }

    @Test
    void deberiaAgregarRellenarYPonerNotNullModoHero() {
        migrador.run(null);

        // La columna existe, quedo rellenada con el default y con NOT NULL.
        assertEquals("UNICO", jdbc.queryForObject("SELECT modo_hero FROM configuracion WHERE id = 1", String.class));
        assertThrows(Exception.class,
                () -> jdbc.update("INSERT INTO configuracion (id, nombre_acueducto, modo_hero) VALUES (2, 'X', NULL)"),
                "modo_hero debe ser NOT NULL");
    }

    @Test
    void deberiaAgregarAuditoriaActivaYColumnasSinDefault() {
        migrador.run(null);

        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT auditoria_activa FROM configuracion WHERE id = 1", Boolean.class));
        // hero_rotacion_actual_id se agrega como columna (nullable, sin default), sin romper.
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'configuracion' AND LOWER(column_name) = 'hero_rotacion_actual_id'", Integer.class));
    }

    @Test
    void deberiaAgregarColumnaIndependienteALaTablaMulta() {
        // Simula una tabla multa con filas existentes pero sin la columna independiente.
        jdbc.execute("CREATE TABLE multa (id BIGINT PRIMARY KEY)");
        jdbc.execute("INSERT INTO multa (id) VALUES (10)");

        migrador.run(null);

        assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT independiente FROM multa WHERE id = 10", Boolean.class));
    }

    @Test
    void deberiaSerIdempotente() {
        migrador.run(null);
        migrador.run(null);
        migrador.run(null);

        assertEquals("UNICO", jdbc.queryForObject("SELECT modo_hero FROM configuracion WHERE id = 1", String.class));
    }

}
