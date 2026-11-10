package ar.unsam.vv.atalaya.api;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.dominio.Rol;
import ar.unsam.vv.atalaya.persistencia.AlertaEntidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * RF-08 · A5 · Traducción de los conflictos de concurrencia a 409, de forma determinista. El
 * servicio de alertas es un mock ({@code @MockBean}) que lanza la misma excepción que lanza JPA
 * cuando dos reconocimientos chocan. CP-25 prueba lo mismo con dos hilos reales, pero depende de
 * que los hilos se crucen (por eso se repite 20 veces); esta prueba fija el comportamiento de la
 * API sin depender del azar.
 */
class ConflictoConcurrenteApiTest extends PruebaApiQa {

    @MockBean
    private ServicioAlertas servicioAlertas;

    @Test
    @DisplayName("CP-25 · choque de versión (@Version) al reconocer → 409 CONFLICTO")
    void choqueDeVersion() throws Exception {
        when(servicioAlertas.reconocer(anyLong(), anyString(), eq(Rol.OPERADOR_REACTOR)))
                .thenThrow(new ObjectOptimisticLockingFailureException(AlertaEntidad.class, 7L));

        reconocer(7, "operador")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
        verify(servicioAlertas).reconocer(7L, "operador", Rol.OPERADOR_REACTOR);
    }

    @Test
    @DisplayName("CP-25 · choque con la columna única de reconocimiento → 409 CONFLICTO")
    void choqueDeColumnaUnica() throws Exception {
        when(servicioAlertas.reconocer(anyLong(), anyString(), eq(Rol.JEFE_TURNO)))
                .thenThrow(new DataIntegrityViolationException("alerta_id duplicado"));

        reconocer(7, "jefe")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
        verify(servicioAlertas).reconocer(7L, "jefe", Rol.JEFE_TURNO);
    }
}
