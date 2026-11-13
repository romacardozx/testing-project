# ATALAYA 3.1.0 · TP integrador VyV

Trabajo práctico integrador de Verificación y Validación de Software (UNSAM · Grupo E · Turno noche).
Implementación reducida de ATALAYA (sistema ficticio de asistencia a la operación de una central
nuclear) sobre la que se aplican las técnicas y herramientas de testing de la materia.

Stack: Java 21 · Spring Boot 3 · Maven · H2 · JUnit 5 · MockMvc · Cucumber · Selenium · PIT · JaCoCo · Postman.

## Requisitos

- Java 21 instalado. Puede no ser el `java` por defecto: el Makefile busca un JDK 21 (`JAVA_HOME`,
  `/usr/lib/jvm`, Homebrew, SDKMAN, `~/.jdks` de IntelliJ, macOS) y lo usa para todo.
- bash, make y curl (Linux, macOS, o WSL / Git Bash en Windows).
- Node.js 18 o más nuevo, solo para Postman (newman).
- Maven **no** hace falta: se usa el wrapper `./mvnw`. Chrome tampoco: Selenium Manager lo baja.

## Primera vez

```bash
make install           # verifica Java 21, baja dependencias y plugins de Maven (Surefire, PIT) e instala newman
make install-chrome    # lo mismo y además baja Chrome for Testing + chromedriver (≈150 MB) para Selenium
make test              # comprobar que todo pasa
```

newman queda instalado localmente en `node_modules/` con la versión fija de `package-lock.json`
(no hace falta instalarlo global).

## Comandos (Makefile)

`make` sin argumentos lista todos los comandos. Los nombres siguen la convención habitual
(`install`, `test`, `run`, `clean`, `dist`).

### Pruebas

| Comando | Qué corre |
|---|---|
| `make test` | Suite completa (`mvn verify`): unitarias, API, integración, BDD y Selenium, más el resumen de cobertura JaCoCo. Con `T=<Clase>`, solo esa clase o patrón |
| `make test-unit` | Unitarias de dominio: caja negra, caja blanca y TDD (paquetes `dominio` y `config`; las de `dominio` son las que usa PIT), y servicios aislados con Mockito (`aplicacion`) |
| `make test-api` | API con MockMvc y contra el servidor real: CP-01, CP-03, CP-13 a CP-15, CP-24, CP-34, CP-35 |
| `make test-integration` | Integración con H2 y reloj de QA: caja gris, concurrencia (CP-25) y pairwise (CP-31) |
| `make test-bdd` | Escenarios Cucumber en español: CP-07, CP-08, CP-21, CP-23, CP-27, CP-28, CP-33 |
| `make test-ui` | Selenium sobre el panel con HtmlUnit, sin navegador: UI-01 a UI-03 |
| `make test-ui-chrome` | Selenium con Chrome visible; cada ventana se cierra al terminar la prueba |
| `make test-ui-demo` | Selenium con Chrome en **modo pausa**: al terminar cada prueba el navegador queda abierto en el estado final, con una franja verde (pasó) o roja (falló), y el SISTEMA sigue levantado para seguir navegando. Cerrar la ventana continúa con la siguiente (o pasan 10 min). Para la demo de la exposición |
| `make test-postman` | Corre la colección de Postman con newman. Si la app no está levantada, la levanta con el perfil qa, corre la colección y la apaga. Deja el resultado en `target/newman/postman.xml` |
| `make mutation` | Mutation testing con PIT sobre `dominio`: score total y por clase, y lista de mutantes vivos |
| `make verify` | `mvn verify` con la salida estándar de Maven, tal cual lo corre la cátedra en un clon limpio |
| `make report` | Abre en el navegador el reporte HTML de la última corrida (ver "Reporte HTML") |

### SISTEMA y entrega

| Comando | Qué hace |
|---|---|
| `make run` | Levanta el SISTEMA con el perfil qa en http://localhost:8080 (panel en `/panel`) |
| `make evidence` | Corre la suite, PIT, Postman y Selenium en Chrome sin ventana, y guarda la salida y las capturas en `docs/evidencias/` |
| `make docs` | Genera `docs/TP_ATALAYA_QA_v7.pdf` desde su fuente HTML (`docs/fuentes/`) |
| `make dist` | Corre la suite completa y PIT; si todo pasa, copia los reportes a `reportes/` y arma `VV-Q2-2026-TN-GrupoE-ATALAYA.zip` con el proyecto, los reportes y `docs/`, sin archivos de trabajo internos ni configuración del IDE. Con `SKIP_TESTS=1` reusa los resultados de `target/` |

### Entorno

| Comando | Qué hace |
|---|---|
| `make install` | Verifica Java 21, descarga dependencias y plugins de Maven (incluye los que solo se bajan al ejecutar: proveedor JUnit de Surefire y PIT) e instala newman con `npm ci` |
| `make install-chrome` | Igual que `make install`, y además baja Chrome y chromedriver para `make test-ui-chrome` / `make test-ui-demo` |
| `make clean` | Borra todo lo generado: `target/` (resultados, reportes, PIT, logs, reporte HTML), `reportes/` y el ZIP de la entrega |
| `make distclean` | `make clean` y además `node_modules/` (newman). Después hace falta `make install`. El Chrome de Selenium queda en `~/.cache/selenium` |

### Variables

| Variable | Uso | Ejemplo |
|---|---|---|
| `T` | Clase o patrón para `make test`; en `make test-ui-demo`, una prueba puntual | `make test T=ServicioEscalamientoTest` |
| `OPEN` | Al terminar, arma y abre el reporte HTML (también si hubo fallos). Sirve en cualquier `make test*` y en `make mutation` | `make test OPEN=1` |
| `HEADLESS` | Chrome sin ventana en `make test-ui-chrome` | `make test-ui-chrome HEADLESS=true` |
| `SKIP_TESTS` | `make dist` sin volver a correr la suite | `make dist SKIP_TESTS=1` |
| `NO_COLOR` | Salida sin colores (por ejemplo, para guardarla como evidencia) | `NO_COLOR=1 make test > evidencia.txt` |

### Ejemplos

```bash
make test T=ValidadorRedundanciaTest         # TDD de RF-03
make test T=ServicioEscalamientoTest         # TDD de RF-08
make test T=PairwiseIntegracionTest          # CP-31
make test T='Validador*Test'                 # patrón
make test OPEN=1                             # suite completa y reporte HTML en el navegador
make test-ui-demo                            # demo: Chrome queda abierto al final de cada prueba
make test-ui-demo T='PanelUiTest#reconocerDesdeElPanel'   # demo de una sola prueba (UI-01)
NO_COLOR=1 make test-unit > docs/evidencias/unitarias.txt
```

## Salida en consola

Los comandos de pruebas muestran un árbol por clase (o por feature de Cucumber) con cada caso marcado
✔ / ✘ / ↷ y su `@DisplayName`, que incluye el ID del caso (CP-xx, UI-xx; INF-xx para las pruebas de
la infraestructura de pruebas; en Cucumber, el tag `@CP-xx` del escenario). Al final, un recuadro con
los totales y:

- casos, fallos y tiempo por tipo de prueba (cuando corre más de un tipo);
- cuántos IDs de caso distintos se ejecutaron y qué defectos tienen prueba de regresión (`[defecto ATA-n, corregido]`);
- las tres clases más lentas;
- un aviso si alguna prueba no tiene ID de caso;
- la lista de fallos con su mensaje.

```
▶ ValidadorRangoTest
  ▸ limitesDeTemperatura(BigDecimal, boolean)
    ✔ CP-01 · temperatura 19.9 °C → dentro de rango: false  27 ms
    ✔ CP-01 · temperatura 20 °C → dentro de rango: true  0 ms

╔══════════════════════════════════════════════════════════════╗
║  RESULTADO: TODO EN VERDE                                    ║
╠══════════════════════════════════════════════════════════════╣
║  Total 307   ✔ 307   ✘ 0     ↷ 0      25,2 s                 ║
╚══════════════════════════════════════════════════════════════╝

  Por tipo                            Casos      ✔      ✘    Tiempo
  Unitarias · dominio                   109    109      0      0,2 s
  Unitarias · servicios (Mockito)         6      6      0      0,1 s
  Infraestructura de pruebas              3      3      0      0,0 s
  API (MockMvc y servidor real)          92     92      0      8,5 s
  Integración (H2, reloj de QA)          81     81      0     13,4 s
  BDD (Cucumber)                         11     11      0      2,0 s
  Interfaz (Selenium)                     5      5      0      0,9 s

  IDs de caso ejecutados: 41 CP · 3 UI · 2 INF
  Regresiones de defectos: ATA-16, ATA-17, ATA-18  (docs/defectos.md)
  Clases más lentas: PairwiseIntegracionTest 5,5 s · SeguridadServidorRealTest 2,8 s · ManiobrasIntegracionTest 2,5 s
```

## Reporte HTML

Con `OPEN=1` (por ejemplo `make test OPEN=1` o `make test-unit OPEN=1`), o después con `make report`,
se arma `target/reporte-pruebas/index.html` y se abre en el navegador por defecto. Es una página
estática, sin internet, con:

- estado general, totales, duración, cobertura de `dominio` y mutation score con sus umbrales;
- casos por tipo de prueba;
- los 46 IDs del catálogo (CP-01 a CP-41, UI-01 a UI-03, INF-01 e INF-02) en verde, rojo o gris (no ejecutado en esa corrida);
- los fallos con su mensaje;
- el detalle por clase o feature, con filtro por texto (ID, clase, descripción, `ATA-n`) y "solo fallos";
- cobertura JaCoCo por paquete y PIT por clase con los mutantes vivos, si existen en `target/`;
- enlaces a los reportes completos de JaCoCo, PIT y Cucumber.

Los resultados los deja el listener de consola (`target/reporte-pruebas/pruebas.js`); la cobertura y
PIT salen de la última corrida de `make test` y `make mutation` (la página muestra su fecha). La
plantilla está en `scripts/reporte/index.html` y el armado en `scripts/reporte.sh`.

## Maven directo (sin Makefile)

`./mvnw` usa el `java` por defecto. Si no es el 21, apuntar antes `JAVA_HOME` a un JDK 21
(`scripts/java21.sh` imprime cuál encontró):

```bash
export JAVA_HOME="$(scripts/java21.sh)"
```

En IntelliJ: *Project Structure → Project SDK* y *Settings → Build Tools → Maven → Runner → JRE* en 21.

```bash
./mvnw verify                                                   # suite completa + JaCoCo
./mvnw test -Dtest=ClasificadorNivelTest                        # una clase
./mvnw test -Dtest='**/integracion/*Test'                       # un paquete
./mvnw test -Dtest='**/ui/*Test' -Dbrowser=chrome               # Selenium con Chrome
./mvnw test -Dtest='**/ui/*Test' -Dbrowser=chrome -Dpausa=true  # Chrome en modo pausa
./mvnw test -Dconsola=true                                      # con el resumen visual
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage    # PIT (test-compile primero)
./mvnw spring-boot:run -Dspring-boot.run.profiles=qa            # SISTEMA con perfil qa
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod          # perfil prod: sin endpoints de QA ni usuarios
```

## Reportes

| Reporte | Ubicación |
|---|---|
| Cobertura JaCoCo | `target/site/jacoco/index.html` |
| Mutation testing PIT | `target/pit-reports/index.html` (log completo en `target/pit.log`) |
| Cucumber | `target/cucumber-reports/cucumber.html` |
| Surefire (JUnit) | `target/surefire-reports/` |
| Postman (newman) | `target/newman/postman.xml` (log de la app en `target/newman/app.log`) |

## Uso manual del SISTEMA (perfil qa)

Usuarios de prueba (datos sintéticos):

| Usuario | Clave | Rol |
|---|---|---|
| `adquisicion` | `adquisicion123` | ADQUISICION |
| `operador` | `operador123` | OPERADOR_REACTOR |
| `jefe` | `jefe123` | JEFE_TURNO |
| `supervisor` | `supervisor123` | SUPERVISOR_SO |
| `auditor` | `auditor123` | AUDITOR |

La API usa HTTP Basic y el panel (`/panel`), login por formulario. Endpoints de preparación, solo con
el perfil qa y sin credenciales:

```bash
curl -X POST localhost:8080/api/v1/qa/reset                                   # base limpia y reloj en T0
curl -X POST localhost:8080/api/v1/qa/reloj -H 'Content-Type: application/json' \
     -d '{"segundos": 45}'                                                     # avanza el reloj y evalúa escalamiento
curl -X POST localhost:8080/api/v1/qa/estimaciones -H 'Content-Type: application/json' \
     -d '{"tipo": "PROCESO", "probabilidad": 80}'                              # inyecta una estimación
curl -u operador:operador123 localhost:8080/api/v1/alertas                     # consulta alertas
```

## Postman

- Colección: `postman/ATALAYA.postman_collection.json`, con una carpeta por requerimiento.
- Entorno: `postman/qa.postman_environment.json` (`baseUrl` y usuarios por rol).
- Por consola: `make test-postman` (levanta y apaga la app sola; resultado JUnit en `target/newman/postman.xml`).
  También `npm run postman` con la app ya levantada.
- Desde Postman: `make run`, importar ambos archivos y correr cada carpeta con el Collection Runner.
- CP-27 repite una request 10 veces con `postman.setNextRequest`: correrlo con el Runner o con newman,
  no enviando las requests de a una.
- La colección se genera con `python3 postman/generar_coleccion.py postman`: regenerarla en vez de editar el JSON a mano.

## Documentación

- `docs/TP_ATALAYA_QA_v7.pdf`: informe (estrategia, casos, criterios y resultados).
- `docs/Matriz_Trazabilidad_ATALAYA.xlsx`: casos de prueba, resultados, trazabilidad y defectos.
- `docs/defectos.md`: registro de defectos (ATA-1 a ATA-18).
- `docs/evidencias/`: salida de cada ejecución (`ejecucion/`, `postman/`), capturas de Selenium y rojo/verde de TDD y de defectos.
- `docs/pit-analisis.md`: análisis de mutantes sobrevivientes.
- `docs/uso-ia.md`: registro del uso de IA (condición 11.f).
- `CLAUDE.md`: reglas de negocio, decisiones del equipo y convenciones del proyecto.
