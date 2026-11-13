package ar.unsam.vv.atalaya.consola;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Resumen visual de la ejecución en consola: árbol de clases (o features de Cucumber) con cada caso
 * marcado ✔ / ✘ / ↷ y su {@code @DisplayName}, y al final un recuadro con totales, el detalle por
 * tipo de prueba, los IDs de caso ejecutados, las regresiones de defectos, las clases más lentas, las
 * pruebas sin ID de caso y los fallos.
 *
 * <p>Además deja los resultados en {@code target/reporte-pruebas/pruebas.js}, que usa el reporte
 * HTML ({@code make report} o {@code OPEN=1}).
 *
 * <p>Se registra por ServiceLoader y solo actúa con {@code -Dconsola=true} (lo pasa el Makefile); con
 * {@code NO_COLOR} definido no usa colores. No es una herramienta de testing: solo formatea la salida
 * de JUnit Platform.
 */
public class ResumenConsola implements TestExecutionListener {

    private static final boolean ACTIVO = Boolean.getBoolean("atalaya.consola");
    private static final boolean COLOR = System.getenv("NO_COLOR") == null;
    private static final Path DATOS = Path.of("target", "reporte-pruebas", "pruebas.js");

    private static final String RESET = "\u001B[0m";
    private static final String NEGRITA = "\u001B[1m";
    private static final String TENUE = "\u001B[2m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CIAN = "\u001B[36m";

    private static final Pattern ID_CASO = Pattern.compile("\\b(CP|UI|INF)-\\d{2}\\b");
    private static final Pattern DEFECTO = Pattern.compile("\\[defecto (ATA-\\d+)(, corregido)?]");

    /** Tipo de prueba según el paquete de la clase de test, en el orden en que se muestran. */
    private static final Map<String, String> TIPOS = new LinkedHashMap<>();

    static {
        TIPOS.put("dominio", "Unitarias · dominio");
        TIPOS.put("aplicacion", "Unitarias · servicios (Mockito)");
        TIPOS.put("config", "Infraestructura de pruebas");
        TIPOS.put("api", "API (MockMvc y servidor real)");
        TIPOS.put("integracion", "Integración (H2, reloj de QA)");
        TIPOS.put("bdd", "BDD (Cucumber)");
        TIPOS.put("ui", "Interfaz (Selenium)");
    }

    private final PrintStream salida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private final Map<UniqueId, Long> inicios = new HashMap<>();
    private final List<String> fallos = new ArrayList<>();
    private final List<Caso> casos = new ArrayList<>();
    private final Map<String, Long> msPorGrupo = new LinkedHashMap<>();
    private TestPlan plan;
    private long inicioPlan;
    private ZonedDateTime fecha;
    private int aprobados;
    private int fallidos;
    private int omitidos;

    /** Resultado de una prueba, tal como se guarda para el reporte HTML. */
    record Caso(String grupo, String tipo, String nombre, List<String> ids, String estado, long ms, String mensaje) {
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!ACTIVO) {
            return;
        }
        plan = testPlan;
        inicioPlan = System.nanoTime();
        fecha = ZonedDateTime.now(Clock.systemDefaultZone());
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
        registrar(id, "OMITIDO", 0, motivo);
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
        if (id.isContainer() && !esMotor(id) && nivel(id) == 0) {
            msPorGrupo.put(id.getDisplayName(), ms);
        }
        if (id.isTest()) {
            if (fallo) {
                fallidos++;
                salida.println(sangria(nivel(id)) + c(ROJO + NEGRITA, "✘ " + id.getDisplayName()) + c(TENUE, "  " + ms + " ms"));
                registrarFallo(id, resultado);
                registrar(id, "FALLO", ms, mensaje(resultado));
            } else {
                aprobados++;
                salida.println(sangria(nivel(id)) + c(VERDE, "✔ ") + id.getDisplayName() + c(TENUE, "  " + ms + " ms"));
                registrar(id, "OK", ms, null);
            }
        } else if (fallo && !esMotor(id)) {
            // Falla del contenedor (por ejemplo, el contexto de Spring no levanta): no hay casos que la muestren.
            fallidos++;
            salida.println(sangria(nivel(id)) + c(ROJO + NEGRITA, "✘ " + id.getDisplayName() + " (no se pudo ejecutar)"));
            registrarFallo(id, resultado);
            registrar(id, "FALLO", ms, mensaje(resultado));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (!ACTIVO) {
            return;
        }
        long msTotal = Duration.ofNanos(System.nanoTime() - inicioPlan).toMillis();
        int total = aprobados + fallidos + omitidos;
        String color = fallidos == 0 ? VERDE : ROJO;
        salida.println();
        salida.println(c(color + NEGRITA, "╔══════════════════════════════════════════════════════════════╗"));
        salida.println(c(color + NEGRITA, linea(fallidos == 0 ? "  RESULTADO: TODO EN VERDE" : "  RESULTADO: HAY FALLOS")));
        salida.println(c(color + NEGRITA, "╠══════════════════════════════════════════════════════════════╣"));
        salida.println(c(color + NEGRITA, linea(String.format("  Total %-5d ✔ %-5d ✘ %-5d ↷ %-5d  %.1f s",
                total, aprobados, fallidos, omitidos, msTotal / 1000.0))));
        salida.println(c(color + NEGRITA, "╚══════════════════════════════════════════════════════════════╝"));
        imprimirPorTipo();
        imprimirIdsYDefectos();
        imprimirMasLentas();
        imprimirSinId();
        if (!fallos.isEmpty()) {
            salida.println();
            salida.println(c(ROJO + NEGRITA, "Fallos:"));
            fallos.forEach(salida::println);
        }
        salida.println();
        guardarDatos(msTotal);
    }

    // ---------------------------------------------------------------------------------------------
    // Secciones del resumen

    private void imprimirPorTipo() {
        Map<String, int[]> cuentas = new LinkedHashMap<>();
        Map<String, Long> tiempos = new HashMap<>();
        for (Caso caso : casos) {
            int[] cuenta = cuentas.computeIfAbsent(caso.tipo(), t -> new int[3]);
            cuenta[switch (caso.estado()) { case "OK" -> 0; case "FALLO" -> 1; default -> 2; }]++;
        }
        casos.stream().map(Caso::grupo).distinct().forEach(grupo -> {
            String tipo = casos.stream().filter(k -> k.grupo().equals(grupo)).findFirst().orElseThrow().tipo();
            tiempos.merge(tipo, msPorGrupo.getOrDefault(grupo, 0L), Long::sum);
        });
        if (cuentas.size() < 2) {
            return;
        }
        salida.println();
        salida.println(c(NEGRITA, String.format("  %-34s %6s %6s %6s %9s", "Por tipo", "Casos", "✔", "✘", "Tiempo")));
        ordenTipos(cuentas.keySet()).forEach(tipo -> {
            int[] cuenta = cuentas.get(tipo);
            String fila = String.format("  %-34s %6d %6d %6d %8.1f s", TIPOS.getOrDefault(tipo, tipo),
                    cuenta[0] + cuenta[1] + cuenta[2], cuenta[0], cuenta[1], tiempos.getOrDefault(tipo, 0L) / 1000.0);
            salida.println(cuenta[1] > 0 ? c(ROJO, fila) : fila);
        });
    }

    private void imprimirIdsYDefectos() {
        Map<String, TreeSet<String>> porPrefijo = new LinkedHashMap<>();
        for (String prefijo : List.of("CP", "UI", "INF")) {
            porPrefijo.put(prefijo, new TreeSet<>());
        }
        casos.forEach(caso -> caso.ids().forEach(id -> porPrefijo.get(id.substring(0, id.indexOf('-'))).add(id)));
        List<String> partes = new ArrayList<>();
        porPrefijo.forEach((prefijo, ids) -> {
            if (!ids.isEmpty()) {
                partes.add(ids.size() + " " + prefijo);
            }
        });
        TreeMap<Integer, String> defectos = new TreeMap<>();
        casos.forEach(caso -> {
            Matcher m = DEFECTO.matcher(caso.nombre());
            while (m.find()) {
                int numero = Integer.parseInt(m.group(1).substring(4));
                defectos.put(numero, m.group(1) + (m.group(2) == null ? " (abierto)" : ""));
            }
        });
        salida.println();
        if (!partes.isEmpty()) {
            salida.println("  " + c(NEGRITA, "IDs de caso ejecutados: ") + String.join(" · ", partes));
        }
        if (!defectos.isEmpty()) {
            salida.println("  " + c(NEGRITA, "Regresiones de defectos: ") + String.join(", ", defectos.values())
                    + c(TENUE, "  (docs/defectos.md)"));
        }
    }

    private void imprimirMasLentas() {
        if (msPorGrupo.size() < 4) {
            return;
        }
        List<String> lentas = msPorGrupo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> e.getKey() + " " + String.format("%.1f s", e.getValue() / 1000.0))
                .toList();
        salida.println("  " + c(NEGRITA, "Clases más lentas: ") + c(TENUE, String.join(" · ", lentas)));
    }

    private void imprimirSinId() {
        List<Caso> sinId = casos.stream().filter(caso -> caso.ids().isEmpty()).toList();
        if (sinId.isEmpty()) {
            return;
        }
        salida.println();
        salida.println(c(AMARILLO + NEGRITA, "  ! " + sinId.size() + " prueba(s) sin ID de caso (CP-xx, UI-xx o INF-xx) en el @DisplayName:"));
        sinId.stream().limit(5).forEach(caso -> salida.println(c(AMARILLO, "    " + caso.grupo() + " › " + caso.nombre())));
    }

    // ---------------------------------------------------------------------------------------------
    // Registro de resultados y datos para el reporte HTML

    private void registrar(TestIdentifier id, String estado, long ms, String mensaje) {
        List<TestIdentifier> camino = camino(id);
        if (camino.isEmpty()) {
            return;
        }
        TestIdentifier grupo = camino.get(0);
        String nombre = camino.size() == 1 ? grupo.getDisplayName()
                : String.join(" › ", camino.subList(1, camino.size()).stream().map(TestIdentifier::getDisplayName).toList());
        TreeSet<String> ids = new TreeSet<>();
        for (TestIdentifier parte : camino) {
            Matcher m = ID_CASO.matcher(parte.getDisplayName());
            while (m.find()) {
                ids.add(m.group());
            }
            parte.getTags().forEach(tag -> {
                if (ID_CASO.matcher(tag.getName()).matches()) {
                    ids.add(tag.getName());
                }
            });
        }
        casos.add(new Caso(grupo.getDisplayName(), tipo(grupo), nombre, List.copyOf(ids), estado, ms, mensaje));
    }

    private void guardarDatos(long msTotal) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("fecha", fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        datos.put("objetivo", System.getProperty("atalaya.objetivo", ""));
        datos.put("java", System.getProperty("java.version"));
        datos.put("ms", msTotal);
        datos.put("aprobados", aprobados);
        datos.put("fallidos", fallidos);
        datos.put("omitidos", omitidos);
        datos.put("tipos", TIPOS);
        datos.put("msPorGrupo", msPorGrupo);
        datos.put("casos", casos);
        try {
            Files.createDirectories(DATOS.getParent());
            Files.writeString(DATOS, "window.ATALAYA_PRUEBAS = " + new ObjectMapper().writeValueAsString(datos) + ";\n");
        } catch (IOException e) {
            salida.println(c(AMARILLO, "  ! no se pudo guardar " + DATOS + ": " + e.getMessage()));
        }
    }

    private void registrarFallo(TestIdentifier id, TestExecutionResult resultado) {
        fallos.add(c(ROJO, "  ✘ " + ruta(id)) + "\n" + c(TENUE, "      " + mensaje(resultado)));
    }

    private static String mensaje(TestExecutionResult resultado) {
        String mensaje = resultado.getThrowable()
                .map(t -> t.getClass().getSimpleName() + ": " + Optional.ofNullable(t.getMessage()).orElse(""))
                .orElse("sin detalle");
        return mensaje.strip().lines().limit(3).reduce((a, b) -> a + " " + b.strip()).orElse("");
    }

    /** Ancestros sin los motores, de la clase (o suite) hasta la prueba. */
    private List<TestIdentifier> camino(TestIdentifier id) {
        List<TestIdentifier> partes = new ArrayList<>();
        Optional<TestIdentifier> actual = Optional.of(id);
        while (actual.isPresent()) {
            if (!esMotor(actual.get())) {
                partes.add(0, actual.get());
            }
            actual = plan.getParent(actual.get());
        }
        return partes;
    }

    /** "Clase › método › caso", sin los motores. */
    private String ruta(TestIdentifier id) {
        return String.join(" › ", camino(id).stream().map(TestIdentifier::getDisplayName).toList());
    }

    /** Paquete de la clase de test bajo ar.unsam.vv.atalaya (dominio, api, bdd, ...). */
    private static String tipo(TestIdentifier grupo) {
        return grupo.getSource()
                .map(fuente -> fuente instanceof ClassSource clase ? clase.getClassName()
                        : fuente instanceof MethodSource metodo ? metodo.getClassName() : null)
                .map(clase -> clase.replace("ar.unsam.vv.atalaya.", ""))
                .map(clase -> clase.contains(".") ? clase.substring(0, clase.indexOf('.')) : "otros")
                .orElse("otros");
    }

    private static List<String> ordenTipos(java.util.Set<String> tipos) {
        List<String> claves = new ArrayList<>(TIPOS.keySet());
        return tipos.stream()
                .sorted(Comparator.comparingInt(t -> claves.contains(t) ? claves.indexOf(t) : claves.size()))
                .toList();
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
