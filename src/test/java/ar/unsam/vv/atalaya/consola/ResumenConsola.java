package ar.unsam.vv.atalaya.consola;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Resumen visual de la ejecución en consola: árbol de clases (o features de Cucumber) con cada caso
 * marcado ✔ / ✘ / ↷ y su {@code @DisplayName}, y al final un recuadro con totales y fallos. Se
 * registra por ServiceLoader y solo actúa con {@code -Dconsola=true} (lo pasa el Makefile); con
 * {@code NO_COLOR} definido no usa colores. No es una herramienta de testing: solo formatea la salida
 * de JUnit Platform.
 */
public class ResumenConsola implements TestExecutionListener {

    private static final boolean ACTIVO = Boolean.getBoolean("atalaya.consola");
    private static final boolean COLOR = System.getenv("NO_COLOR") == null;

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String TENUE = "\u001B[2m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CIAN = "\u001B[36m";

    private final PrintStream salida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private final Map<UniqueId, Long> inicios = new HashMap<>();
    private final List<String> fallos = new ArrayList<>();
    private TestPlan plan;
    private long inicioPlan;
    private int aprobados;
    private int fallidos;
    private int omitidos;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!ACTIVO) {
            return;
        }
        plan = testPlan;
        inicioPlan = System.nanoTime();
        salida.println();
        salida.println(c(CIAN + NEGRITA, "┌──────────────────────────────────────────────────────────────┐"));
        salida.println(c(CIAN + NEGRITA, "│  ATALAYA 3.1.0 · ejecución de pruebas                         │"));
        salida.println(c(CIAN + NEGRITA, "└──────────────────────────────────────────────────────────────┘"));
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        if (!ACTIVO) {
            return;
        }
        inicios.put(UniqueId.parse(id.getUniqueId()), System.nanoTime());
        if (id.isContainer() && !esMotor(id)) {
            int nivel = nivel(id);
            if (nivel == 0) {
                salida.println();
                salida.println(c(NEGRITA + CIAN, "▶ " + id.getDisplayName()));
            } else {
                salida.println(sangria(nivel) + c(TENUE, "▸ " + id.getDisplayName()));
            }
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String motivo) {
        if (!ACTIVO || !id.isTest()) {
            return;
        }
        omitidos++;
        salida.println(sangria(nivel(id)) + c(AMARILLO, "↷ " + id.getDisplayName()) + c(TENUE, "  (" + motivo + ")"));
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult resultado) {
        if (!ACTIVO) {
            return;
        }
        long ms = Duration.ofNanos(System.nanoTime()
                - inicios.getOrDefault(UniqueId.parse(id.getUniqueId()), System.nanoTime())).toMillis();
        boolean fallo = resultado.getStatus() != TestExecutionResult.Status.SUCCESSFUL;
        if (id.isTest()) {
            if (fallo) {
                fallidos++;
                salida.println(sangria(nivel(id)) + c(ROJO + NEGRITA, "✘ " + id.getDisplayName()) + c(TENUE, "  " + ms + " ms"));
                registrarFallo(id, resultado);
            } else {
                aprobados++;
                salida.println(sangria(nivel(id)) + c(VERDE, "✔ ") + id.getDisplayName() + c(TENUE, "  " + ms + " ms"));
            }
        } else if (fallo && !esMotor(id)) {
            // Falla del contenedor (por ejemplo, el contexto de Spring no levanta): no hay casos que la muestren.
            fallidos++;
            salida.println(sangria(nivel(id)) + c(ROJO + NEGRITA, "✘ " + id.getDisplayName() + " (no se pudo ejecutar)"));
            registrarFallo(id, resultado);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (!ACTIVO) {
            return;
        }
        double segundos = (System.nanoTime() - inicioPlan) / 1_000_000_000.0;
        int total = aprobados + fallidos + omitidos;
        String color = fallidos == 0 ? VERDE : ROJO;
        salida.println();
        salida.println(c(color + NEGRITA, "╔══════════════════════════════════════════════════════════════╗"));
        salida.println(c(color + NEGRITA, linea(fallidos == 0 ? "  RESULTADO: TODO EN VERDE" : "  RESULTADO: HAY FALLOS")));
        salida.println(c(color + NEGRITA, "╠══════════════════════════════════════════════════════════════╣"));
        salida.println(c(color + NEGRITA, linea(String.format("  Total %-5d ✔ %-5d ✘ %-5d ↷ %-5d  %.1f s",
                total, aprobados, fallidos, omitidos, segundos))));
        salida.println(c(color + NEGRITA, "╚══════════════════════════════════════════════════════════════╝"));
        if (!fallos.isEmpty()) {
            salida.println();
            salida.println(c(ROJO + NEGRITA, "Fallos:"));
            fallos.forEach(salida::println);
        }
        salida.println();
    }

    private void registrarFallo(TestIdentifier id, TestExecutionResult resultado) {
        String mensaje = resultado.getThrowable()
                .map(t -> t.getClass().getSimpleName() + ": " + Optional.ofNullable(t.getMessage()).orElse(""))
                .orElse("sin detalle");
        String primeraLinea = mensaje.strip().lines().limit(3).reduce((a, b) -> a + " " + b.strip()).orElse("");
        fallos.add(c(ROJO, "  ✘ " + ruta(id)) + "\n" + c(TENUE, "      " + primeraLinea));
    }

    /** "Clase › método › caso", sin los motores. */
    private String ruta(TestIdentifier id) {
        List<String> partes = new ArrayList<>();
        Optional<TestIdentifier> actual = Optional.of(id);
        while (actual.isPresent()) {
            if (!esMotor(actual.get())) {
                partes.add(0, actual.get().getDisplayName());
            }
            actual = plan.getParent(actual.get());
        }
        return String.join(" › ", partes);
    }

    /** Profundidad sin contar los motores (Jupiter, Suite, Cucumber). */
    private int nivel(TestIdentifier id) {
        int nivel = 0;
        Optional<TestIdentifier> padre = plan.getParent(id);
        while (padre.isPresent()) {
            if (!esMotor(padre.get())) {
                nivel++;
            }
            padre = plan.getParent(padre.get());
        }
        return nivel;
    }

    private static boolean esMotor(TestIdentifier id) {
        List<UniqueId.Segment> segmentos = UniqueId.parse(id.getUniqueId()).getSegments();
        return "engine".equals(segmentos.get(segmentos.size() - 1).getType());
    }

    private static String sangria(int nivel) {
        return "  ".repeat(Math.max(nivel, 1));
    }

    private static String linea(String texto) {
        return "║" + String.format("%-62s", texto) + "║";
    }

    private static String c(String estilo, String texto) {
        return COLOR ? estilo + texto + RESET : texto;
    }
}
