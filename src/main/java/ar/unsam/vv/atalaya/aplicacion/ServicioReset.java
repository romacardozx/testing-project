package ar.unsam.vv.atalaya.aplicacion;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja la base vacía (precondición "H2 limpia"). Lo usan /qa/reset y los tests de integración.
 * Vacía todas las tablas de H2, incluidas las de colecciones, sin depender del orden de las FK.
 */
@Service
public class ServicioReset {

    private final JdbcTemplate jdbc;

    public ServicioReset(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void limpiar() {
        List<String> tablas = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'"
                        + " and table_type = 'BASE TABLE'", String.class);
        jdbc.execute("set referential_integrity false");
        tablas.forEach(t -> jdbc.execute("truncate table \"" + t + "\""));
        jdbc.execute("set referential_integrity true");
    }
}
