package ar.unsam.vv.atalaya.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.unsam.vv.atalaya.PruebaApiQa;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * CP-25 · RF-08, A5. Caja gris: el operador y el jefe reconocen la misma alerta a la vez. Un
 * CountDownLatch los larga juntos; se repite 20 veces porque una sola corrida puede no cruzarlos.
 * En cada corrida: una respuesta 200, otra 409 y un solo registro en la tabla de reconocimientos.
 */
class ReconocimientoConcurrenteTest extends PruebaApiQa {

    @Autowired
    private JdbcTemplate jdbc;

    @RepeatedTest(value = 20, name = "CP-25 · dos reconocimientos simultáneos, corrida {currentRepetition}/20")
    @DisplayName("CP-25 · dos reconocimientos simultáneos: uno solo efectivo")
    void unoSoloEfectivo() throws Exception {
        long id = alertaDe("PROCESO", "50");
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> reconocimientos = List.of(
                    () -> reconocerAlLargar(largada, id, "operador"),
                    () -> reconocerAlLargar(largada, id, "jefe"));
            List<Future<Integer>> futuros = reconocimientos.stream().map(hilos::submit).toList();
            largada.countDown();

            List<Integer> estados = futuros.stream().map(ReconocimientoConcurrenteTest::esperar).sorted().toList();

            assertThat(estados).containsExactly(200, 409);
            assertThat(jdbc.queryForObject(
                    "select count(*) from reconocimiento where alerta_id = ?", Integer.class, id)).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }
    }

    private int reconocerAlLargar(CountDownLatch largada, long id, String usuario) throws Exception {
        largada.await();
        return reconocer(id, usuario).andReturn().getResponse().getStatus();
    }

    private static int esperar(Future<Integer> futuro) {
        try {
            return futuro.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
