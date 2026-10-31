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
make instalar          # verifica Java 21, baja dependencias y plugins de Maven (Surefire, PIT) e instala newman
make instalar-chrome   # lo mismo y además baja Chrome for Testing + chromedriver (≈150 MB) para Selenium
make todo              # comprobar que todo pasa
```

newman queda instalado localmente en `node_modules/` con la versión fija de `package-lock.json`
(no hace falta instalarlo global).

## Comandos (Makefile)

`make` sin argumentos lista todos los comandos.

### Instalación

| Comando | Qué hace |
|---|---|
| `make instalar` | Verifica Java 21, descarga dependencias y plugins de Maven (incluye los que solo se bajan al ejecutar: proveedor JUnit de Surefire y PIT) e instala newman con `npm ci` |
| `make instalar-chrome` | Igual que `make instalar`, y además baja Chrome y chromedriver para `make ui-chrome` / `make ui-demo` |

### Pruebas por tipo

| Comando | Qué corre |
|---|---|
| `make todo` | Suite completa (`mvn verify`): unitarias, API, integración, BDD y Selenium, más el resumen de cobertura JaCoCo |
| `make unitarias` | Unitarias de dominio: caja negra, caja blanca y TDD (paquetes `dominio` y `config`). Son las que usa PIT |
| `make api` | API con MockMvc y contra el servidor real: CP-01, CP-03, CP-13 a CP-15, CP-24, CP-34, CP-35 |
| `make integracion` | Integración con H2 y reloj de QA: caja gris, concurrencia (CP-25) y pairwise (CP-31) |
| `make bdd` | Escenarios Cucumber en español: CP-07, CP-08, CP-21, CP-23, CP-27, CP-28, CP-33 |
| `make ui` | Selenium sobre el panel con HtmlUnit, sin navegador: UI-01 a UI-03 |
| `make ui-chrome` | Selenium con Chrome visible; cada ventana se cierra al terminar la prueba |
| `make ui-demo` | Selenium con Chrome en **modo pausa**: al terminar cada prueba el navegador queda abierto en el estado final, con una franja verde (pasó) o roja (falló), y el SISTEMA sigue levantado para seguir navegando. Cerrar la ventana continúa con la siguiente (o pasan 10 min). Para la demo de la exposición |
| `make clase T=<Clase>` | Una clase o un patrón de clases |

### Calidad de las pruebas

| Comando | Qué hace |
|---|---|
| `make pit` | Mutation testing con PIT sobre `dominio`: score total y por clase, y lista de mutantes vivos |
| `make cobertura` | Corre la suite y muestra la cobertura JaCoCo por paquete, con los umbrales del informe (dominio: 80 % líneas, 70 % ramas) |
| `make verify` | `mvn verify` con la salida estándar de Maven, tal cual lo corre la cátedra en un clon limpio |

### SISTEMA, Postman y entrega

| Comando | Qué hace |
|---|---|
| `make app` | Levanta el SISTEMA con el perfil qa en http://localhost:8080 (panel en `/panel`) |
| `make postman` | Corre la colección de Postman con newman. Si la app no está levantada, la levanta con el perfil qa, corre la colección y la apaga. Deja el resultado en `target/newman/postman.xml` |
| `make reportes` | Copia los reportes de JaCoCo, PIT y Cucumber a `reportes/` para armar el ZIP |
| `make limpiar` | Borra `target/` y `reportes/` |

### Variables

| Variable | Uso | Ejemplo |
|---|---|---|
| `T` | Clase o patrón para `make clase`; en `make ui-demo`, una prueba puntual | `make clase T=ServicioEscalamientoTest` |
| `HEADLESS` | Chrome sin ventana en `make ui-chrome` | `make ui-chrome HEADLESS=true` |
| `NO_COLOR` | Salida sin colores (por ejemplo, para guardarla como evidencia) | `NO_COLOR=1 make todo > evidencia.txt` |

### Ejemplos

```bash
make clase T=ValidadorRedundanciaTest        # TDD de RF-03
make clase T=ServicioEscalamientoTest        # TDD de RF-08
make clase T=PairwiseIntegracionTest         # CP-31
make clase T='Validador*Test'                # patrón
make ui-demo                                 # demo: Chrome queda abierto al final de cada prueba
make ui-demo T='PanelUiTest#reconocerDesdeElPanel'   # demo de una sola prueba (UI-01)
NO_COLOR=1 make unitarias > docs/evidencias/unitarias.txt
```

## Salida en consola

Los comandos de pruebas muestran un árbol por clase (o por feature de Cucumber) con cada caso marcado
✔ / ✘ / ↷ y su `@DisplayName`, que incluye el ID del caso (CP-xx, UI-xx). Al final, un recuadro con
los totales y la lista de fallos con su mensaje:

```
▶ ValidadorRangoTest
  ▸ limitesDeTemperatura(BigDecimal, boolean)
    ✔ CP-01 · temperatura 19.9 °C → dentro de rango: false  27 ms
    ✔ CP-01 · temperatura 20 °C → dentro de rango: true  0 ms

╔══════════════════════════════════════════════════════════════╗
║  RESULTADO: TODO EN VERDE                                    ║
╠══════════════════════════════════════════════════════════════╣
║  Total 297   ✔ 297   ✘ 0     ↷ 0      24,5 s                 ║
╚══════════════════════════════════════════════════════════════╝
```

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
- Por consola: `make postman` (levanta y apaga la app sola; resultado JUnit en `target/newman/postman.xml`).
  También `npm run postman` con la app ya levantada.
- Desde Postman: `make app`, importar ambos archivos y correr cada carpeta con el Collection Runner.
- CP-27 repite una request 10 veces con `postman.setNextRequest`: correrlo con el Runner o con newman,
  no enviando las requests de a una.
- La colección se genera con `python3 postman/generar_coleccion.py postman`: regenerarla en vez de
  editar el JSON a mano.

## Documentación

- `docs/TP_ATALAYA_QA_v6.pdf`: informe (estrategia, casos, criterios).
- `docs/Matriz_Trazabilidad_ATALAYA.xlsx`: casos de prueba y trazabilidad.
- `docs/evidencias/tdd/`: salida de cada etapa red/green de TDD.
- `docs/pit-analisis.md`: análisis de mutantes sobrevivientes.
- `docs/uso-ia.md`: registro del uso de IA (condición 11.f).
- `CLAUDE.md`: reglas de negocio, decisiones del equipo y convenciones del proyecto.
