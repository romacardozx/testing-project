# CLAUDE.md · ATALAYA 3.1.0 (TP Integrador VyV · UNSAM · Grupo E · Turno noche)

## Qué es este proyecto

Trabajo práctico integrador de Verificación y Validación de Software (UNSAM). Somos el equipo de QA de
**ATALAYA**, un sistema ficticio de asistencia a la operación de una central nuclear: recibe señales
de planta, clasifica estimaciones de riesgo en niveles y genera alertas para la sala de control.
ATALAYA **no controla nada**: no tiene ningún camino de escritura hacia control ni protección.

El enunciado no trae código. El profesor confirmó que tenemos que construir **una implementación
básica** con endpoints, comportamientos y escenarios concretos para poder aplicar las técnicas de la
materia. **La complejidad del código no es el objetivo**: lo que se evalúa son las pruebas. Mantener
el SISTEMA chico, claro y fácil de probar.

Mensaje del profesor (Di Ludovico, 30/10/2026): implementación básica con endpoints, comportamientos y
escenarios concretos; aplicar casos de prueba, defectos documentados, caja negra, automatización,
**pruebas de interfaz**, Pairwise, TDD, BDD y Mutation Testing con **Selenium, Postman, Cucumber y PIT**.
Por eso el panel web y las pruebas de Selenium **están dentro del alcance** (el informe v6 todavía dice
lo contrario: ver "Pendientes del informe").

Entrega: **13/11/2026** · ZIP `VV-Q2-2026-TN-GrupoE-ATALAYA.zip`.

## Reglas duras (si se rompen, el TP se desaprueba)

1. **Java 21 + Spring Boot 3 + Maven.** Nada de otros lenguajes ni frameworks para el SISTEMA o las pruebas.
2. `mvn verify` tiene que compilar y pasar **en un clon limpio**. Un test fallido solo se admite si
   evidencia un defecto del SISTEMA reportado en Jira (ver "Tests que fallan a propósito").
3. Herramientas de la cátedra: **JUnit 5, Spring Boot Test, MockMvc, Cucumber, PIT, JaCoCo, Postman, Selenium**.
   No sumar otras herramientas de testing sin consultarlo (por ejemplo, no Playwright, RestAssured, k6, WireMock).
4. **Sin datos reales**: todos los datos son sintéticos y llevan semilla o están fijos en el test.
5. **Todo test lleva comentario** (Javadoc en la clase con su propósito y `@DisplayName` con el ID del caso).
6. **Uso de IA**: los lineamientos (condición 11.f) exigen declararlo. Cada vez que generes código o
   pruebas, agregá una línea en `docs/uso-ia.md` (fecha, qué se pidió, qué se generó). El equipo
   completa después qué revisó y qué cambió. No borrar ese archivo.

## Stack y dependencias

- Spring Boot 3.3.x: `web`, `validation`, `data-jpa`, `security`, `thymeleaf`, `actuator`.
- H2 en memoria.
- Tests: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ, MockMvc), `spring-security-test`.
- Cucumber: `cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`, `junit-platform-suite`.
- Selenium: `selenium-java` + `htmlunit3-driver` (corre sin navegador en `mvn verify`). Spring Boot 3.3
  no gestiona `htmlunit3-driver`: su versión va fija en el pom, alineada con la de Selenium (4.19.x).
  Con `-Dbrowser=chrome` se usa ChromeDriver para la demo en la exposición.
- Plugins: `jacoco-maven-plugin` (prepare-agent + report en `verify`, **sin** reglas que rompan el build),
  `pitest-maven` + `pitest-junit5-plugin` (se corre aparte, no en `verify`).
- Sin Lombok. Usar `record` para DTOs.

- Maven Wrapper (`mvnw`) versionado en el repo: nadie necesita Maven instalado.
- El Makefile corre todo con un JDK 21 aunque el `java` por defecto sea otro (hay integrantes con Java
  26 por defecto): `scripts/java21.sh` lo busca y se exportan `JAVA_HOME` y `PATH`. Con `./mvnw`
  directo, exportar antes `JAVA_HOME="$(scripts/java21.sh)"`.

Comandos (preferir el Makefile: muestra un árbol ✔/✘ con los `@DisplayName` y un resumen; `make` sin
argumentos lista todo):
```
make todo | unitarias | api | integracion | bdd | ui | ui-chrome   # por tipo de prueba
make clase T=ServicioEscalamientoTest                             # una clase o patrón
make pit | cobertura                                              # resumen de PIT / JaCoCo con umbrales
make app | postman | reportes | limpiar
make instalar | instalar-chrome                                   # primera vez: dependencias, newman, Chrome
```
El resumen lo imprime `src/test/java/.../consola/ResumenConsola` (listener de JUnit Platform, solo con
`-Dconsola=true`); los resúmenes de PIT y JaCoCo, `scripts/*.sh` (bash + awk). Logs de tests en WARN
(`logback-test.xml`). Si se agrega un test nuevo en otro paquete, sumarlo al patrón del objetivo que
corresponda en el Makefile.

Maven directo:
```
./mvnw verify                                          # todo: unitarias, integración, MockMvc, Cucumber, Selenium (HtmlUnit) + JaCoCo
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage   # PIT sobre dominio (sin test-compile usa tests viejos)
./mvnw spring-boot:run -Dspring-boot.run.profiles=qa   # para Postman y demo manual
```
Reportes: `target/site/jacoco/`, `target/pit-reports/`, `target/cucumber-reports/`. Para la entrega se
copian a `reportes/` dentro del ZIP (no se versionan).

## Estructura

```
pom.xml
src/main/java/ar/unsam/vv/atalaya/
  dominio/        lógica pura, SIN Spring: ValidadorRango, ValidadorRedundancia, ClasificadorNivel,
                  ServicioEscalamiento, PoliticaRepeticion, ReglaManiobra, ValidadorUmbrales, enums (Rol incluido) y records
  aplicacion/     servicios @Service que orquestan dominio + repositorios
  persistencia/   entidades JPA y repositorios
  web/api/        controladores REST + manejo de errores
  web/panel/      controlador Thymeleaf del panel
  web/qa/         endpoints solo del perfil qa
  config/         seguridad, reloj, perfiles
src/main/resources/ application.yml, application-qa.yml, application-prod.yml, templates/panel.html
src/test/java/ar/unsam/vv/atalaya/
  dominio/        unitarias puras (las que corre PIT)
  integracion/    @SpringBootTest + H2 (caja gris)
  api/            @WebMvcTest / MockMvc
  bdd/            runner y steps de Cucumber
  ui/             Selenium (Page Objects en ui/paginas/)
src/test/resources/features/   *.feature en español
src/test/resources/datos/      pairwise.csv y otros datos sintéticos
postman/                       ATALAYA.postman_collection.json, qa.postman_environment.json
docs/                          uso-ia.md, evidencias/ (capturas de TDD, Postman, Selenium)
```

La lógica del paquete `dominio` no depende de Spring. Así se prueba rápido, PIT corre rápido y la
cobertura de caja blanca es clara.

## Alcance

**Se implementa:** RF-01/02, RF-03, RF-06, RF-07/08, RF-09, RF-12, seguridad por rol y un panel web mínimo.

**NO implementar** (quedan fuera, justificado en el informe): modo operativo y calibración (RF-04/05),
modo degradado (RF-10), integraciones con registro de turno y mantenimiento y auditoría (RF-11),
modelo de IA real, desempeño, disponibilidad, red segregada.

Las estimaciones del modelo **no se calculan**: se inyectan por un endpoint del perfil qa.

## Base de datos

H2 embebida con `spring.datasource.generate-unique-name: true`: cada contexto de Spring tiene su base. No
fijar una URL `jdbc:h2:mem:<nombre>`: los tests levantan contextos qa y prod en la misma JVM y, con la
base compartida, el tick de prod (reloj real) escala las alertas de qa (creadas en T0). Pasó el 31/10 y
dio un test que a veces fallaba (CP-18).

## Tiempo

Todo lo que dependa del tiempo recibe un `java.time.Clock` (bean). Prohibido `Instant.now()` o
`LocalDateTime.now()` sin clock. En tests y en el perfil qa se usa un `RelojMutable` (extiende `Clock`)
que se puede avanzar. Nunca usar `Thread.sleep` en tests.

## Reglas de negocio (con las decisiones del equipo ya tomadas)

Las decisiones marcadas Ax resuelven ambigüedades del enunciado. Implementarlas tal cual: los casos
de prueba las verifican.

### RF-01/02 · Lecturas

Magnitudes y rango físico, **inclusivo** en ambos extremos:

| Magnitud | Mín | Máx | Unidad |
|---|---|---|---|
| TEMPERATURA_PRIMARIO | 20 | 350 | °C |
| PRESION_PRIMARIO | 0 | 180 | bar |
| NIVEL_PRESURIZADOR | 0 | 100 | % |
| POTENCIA_TERMICA | 0 | 120 | % nominal |
| TASA_DOSIS | 0 | 10000 | µSv/h |

Lectura: `magnitud`, `canal` (A, B o C), `valor` (decimal), `secuencia` (long), `timestampOrigen`.
Respuesta con `estado`:
- `DESCARTADA_RANGO` si el valor está fuera del rango. No se usa como valor, pero marca el canal (ver RF-03).
- `DUPLICADA` si ya existe la misma magnitud + canal + secuencia. No se guarda de nuevo.
- `FUERA_DE_ORDEN` si la secuencia es menor que la última aceptada de ese canal. No pisa el valor vigente.
- `ACEPTADA` en otro caso.

JSON inválido, valor nulo, valor no numérico, magnitud inexistente o canal fuera de A/B/C → 400.

### RF-03 · Redundancia (tabla de decisión, se evalúa en este orden)

1. Sin lectura aceptada hace **más de 10 s** (10,0 s sigue confiable; 10,1 s no) → `NO_CONFIABLE`, motivo `SIN_ACTUALIZACION`.
2. La última lectura recibida del canal estaba fuera de rango → `NO_CONFIABLE`, motivo `FUERA_DE_RANGO` (A17).
3. |valor − mediana de los canales con valor| **mayor** que el 2 % del rango físico (A1, A2) → `NO_CONFIABLE`, motivo `DISCREPANCIA`.
   Ejemplo: temperatura, rango 330 → límite 6,6 °C. Una desviación de 6,60 es confiable; 6,64 no.
4. Si no, `CONFIABLE`.

**Usar `BigDecimal` en esta comparación.** Con `double`, 306,60 − 300,00 da 6,6000000000000227 y el
caso de 2,00 % (CP-08) falla. Es un defecto real y esperable: si aparece, se reporta.

Recuperación (A3): un canal `NO_CONFIABLE` vuelve a `CONFIABLE` después de **3 lecturas consecutivas**
que cumplen las reglas 1 a 3. Cualquier lectura que no las cumple reinicia el contador.

Decisiones de implementación (31/10, revisarlas con el equipo; si se aceptan, sumarlas a la sección 5
del informe):
- Regla 1 mide la última lectura **recibida** del canal (aceptada o descartada por rango), no solo la
  aceptada. Si no, un canal que envía 351 °C sería SIN_ACTUALIZACION y no FUERA_DE_RANGO (CP-10).
  Las DUPLICADA y FUERA_DE_ORDEN no cuentan como actualización. Canal que nunca envió nada → SIN_ACTUALIZACION.
- Clasificación de una lectura en este orden: DUPLICADA → FUERA_DE_ORDEN → DESCARTADA_RANGO → ACEPTADA.
  Una lectura vieja no marca el canal aunque esté fuera de rango.
- Un canal que vuelve a enviar después de más de 10 s queda marcado SIN_ACTUALIZACION y necesita las
  3 lecturas de A3 para recuperarse. En CP-11, A tiene que seguir enviando durante los 11 s.
- En la consulta, el motivo que falla ahora tiene prioridad sobre el motivo con que quedó marcado.
- Valor consolidado con 2 decimales (HALF_UP). Mediana de 2 valores = su promedio.
- `GET /magnitudes/{magnitud}` acepta el nombre sin distinguir mayúsculas; inexistente → 404. La
  planilla (CP-06) dice `/magnitudes/temperatura`: corregir a `TEMPERATURA_PRIMARIO`.

Magnitud: utilizable si tiene **2 o más** canales confiables. Valor consolidado = promedio de los
confiables. Avisos: `CANAL_EXCLUIDO:<canal>` por cada canal no confiable, y `MAGNITUD_NO_DISPONIBLE`
si hay menos de 2 canales confiables.

### RF-06 · Niveles

```java
public NivelRiesgo clasificar(BigDecimal p, Umbrales u) {
    if (p < 0 || p > 100) throw new ProbabilidadInvalidaException();  // D1
    if (p >= u.critico()) return CRITICO;                              // D2
    if (p >= u.alto())    return ALTO;                                 // D3
    if (p >= 15)          return ATENCION;                             // D4
    return NORMAL;
}
```
Pseudocódigo: en Java se escribe con `compareTo` de BigDecimal. Mantener esta forma (4 decisiones, V(G) = 5): el informe la usa para la caja blanca. La atención va
del 15 % al umbral alto vigente (A7). Umbrales por defecto: alto 40, crítico 75.

Estimación inyectada: `tipo` (PROCESO, FUGA, RADIOLOGICA, BOMBA), `probabilidad` (0 a 100) y
`factores` [{nombre, peso}]. Desde ATENCION se devuelven los **3 factores de mayor peso**, ordenados
de mayor a menor. En NORMAL la lista puede venir vacía.

### RF-07/08 · Alertas y escalamiento

Se genera una alerta por estimación según su nivel:

| Nivel | Canales | Destinatarios | Requiere reconocimiento |
|---|---|---|---|
| NORMAL | (no genera alerta) | | |
| ATENCION | VISUAL | ninguno | no |
| ALTO | VISUAL | OPERADOR_REACTOR | sí |
| CRITICO | VISUAL, SONORA | OPERADOR_REACTOR, JEFE_TURNO | sí |

Repetición, para el mismo `tipo`:
- Mismo nivel dentro de los **180 s** desde la última emisión → no se emite (a 179 s no; a 180 s sí).
- Nivel mayor que la última alerta de ese tipo → se emite siempre (A4: "empeorar" es subir de nivel).
- Mismo nivel con probabilidad mayor → no se emite.
- Nivel **menor** dentro de los 180 s → no se emite (decisión del 31/10: no empeora; sumar a la sección 5).

Escalamiento de alertas no reconocidas (se evalúa con el clock en cada tick y cada vez que se avanza el reloj de QA).
El tick (`TickEscalamiento`, cada 1 s) corre solo fuera del perfil qa; en qa el escalamiento se evalúa en `POST /qa/reloj`:
- CRITICO: a los **≥ 45 s** pasa a `ESCALADA` y suma `SUPERVISOR_SO` a los destinatarios. Una sola vez.
- ALTO: a los **≥ 90 s** pasa a `ESCALADA` y suma `JEFE_TURNO`. Una sola vez.

Estados: `EMITIDA` → `ESCALADA` → `RECONOCIDA`, o `EMITIDA` → `RECONOCIDA`.

Reconocimiento:
- Pueden reconocer OPERADOR_REACTOR, JEFE_TURNO y SUPERVISOR_SO.
- Reconocer una alerta ya `RECONOCIDA` devuelve 409.
- Dos reconocimientos simultáneos (A5): **uno solo** efectivo y el otro recibe 409. Usar `@Version`
  (optimistic locking) en la entidad Alerta y traducir `OptimisticLockingFailureException` a 409.
- Se guarda un `Reconocimiento` (alertaId único, usuario, rol, instante).

### RF-09 · Maniobras

- Las declara el JEFE_TURNO: `magnitud`, `bandaMin`, `bandaMax`, `duracionMaximaMin` (por defecto 30).
- Mientras la maniobra está `ACTIVA`, cada estimación de nivel ≥ ATENCION se evalúa contra el valor
  consolidado de la magnitud de la maniobra:
  - Si está dentro de [bandaMin, bandaMax], inclusive → **no** genera alerta propia; se agrupa en una
    única alerta `INFORMATIVA` de la maniobra (se crea una vez y se incrementa un contador).
  - Si está fuera de la banda, o si la magnitud no es utilizable → alerta normal.
- Los avisos de canal (RF-03) se informan aparte y nunca quedan absorbidos por la agrupación.
- Expiración (A6): cuando `ahora >= declaradaEn + duracionMaxima` pasa a `EXPIRADA` y se vuelve a
  alertar normalmente (a 29:59 se agrupa; a 30:00 no).
- `DELETE` la pasa a `FINALIZADA`.

Decisiones de implementación de RF-09 (31/10, sumar a la sección 5 del informe):
- Hay como mucho **una** maniobra ACTIVA; declarar otra → 409 `MANIOBRA_ACTIVA`. `DELETE` de una
  maniobra que no está ACTIVA → 409 `MANIOBRA_NO_ACTIVA`.
- La alerta INFORMATIVA es una `Alerta` con `clase = INFORMATIVA`, sin tipo ni nivel, visual, sin
  destinatarios ni reconocimiento, con `maniobraId` y contador `agrupadas`. Una por maniobra, agrupa
  estimaciones de cualquier tipo. No escala.
- La expiración se evalúa en el tick, en `POST /qa/reloj` y antes de usar la maniobra activa.

### RF-12 · Umbrales

- Solo SUPERVISOR_SO puede solicitar un cambio. Rangos válidos, inclusivos: alto entre 30 y 50,
  crítico entre 65 y 100 (A8), y alto menor que crítico. Fuera de eso → 422.
- La solicitud queda `PENDIENTE` y no cambia nada hasta que la aprueba un **JEFE_TURNO** (otro rol y otro usuario).
- Con la aprobación queda `APLICADA` y los umbrales vigentes cambian.
- Otros roles → 403. Si aprueba el mismo usuario que la solicitó → 409.
- Hallazgo: con un rol por usuario, "aprueba el mismo usuario" nunca llega al 409 por la API: el
  supervisor que solicita recibe 403 por rol antes. El 409 se prueba llamando al servicio (CP-33).
- Hallazgo: la regla "alto menor que crítico" es inalcanzable (alto ≤ 50 < 65 ≤ crítico). Queda en el
  código porque está en el requerimiento; sus mutantes son equivalentes (docs/pit-analisis.md).
- `PUT /config/umbrales` válido → 201 con la solicitud PENDIENTE.

### Seguridad y roles

API con HTTP Basic y panel con form login. Usuarios en memoria (las contraseñas van en `application-qa.yml`):
`adquisicion` (ADQUISICION), `operador` (OPERADOR_REACTOR), `jefe` (JEFE_TURNO),
`supervisor` (SUPERVISOR_SO), `auditor` (AUDITOR).

| Operación | ADQUISICION | OPERADOR | JEFE | SUPERV. SO | AUDITOR |
|---|---|---|---|---|---|
| POST lecturas | Sí | No | No | No | No |
| GET magnitudes, estimaciones, alertas | No | Sí | Sí | Sí | Sí |
| Reconocer alertas | No | Sí | Sí | Sí | No |
| Declarar o finalizar maniobra | No | No | Sí | No | No |
| Solicitar umbral | No | No | No | Sí | No |
| Aprobar umbral | No | No | Sí | No | No |

Sin credenciales → 401. Con un rol no permitido → 403.

Lo que no figura en la matriz se niega (`denyAll`); `GET /version` lo puede usar cualquier autenticado.
La API escribe 401 y 403 como JSON `{codigo, mensaje, campo}` **sin `sendError`**: con `sendError` el
servidor reenvía a `/error`, lo atiende la cadena del panel y la API respondía 302 al login. MockMvc no
hace ese reenvío; el defecto apareció al probar contra el servidor real (regresión en
`SeguridadServidorRealTest`). Cargarlo en Jira como hallazgo.

Excepción: `/api/v1/qa/**` es `permitAll`. Solo existe con el perfil qa, y así con prod responde 404
y no 401 (CP-36). CP-34 no aplica a esos endpoints.

## API

Base `/api/v1`. Errores en JSON: `{ "codigo": "...", "mensaje": "...", "campo": "..." }`.

| Método y ruta | Uso |
|---|---|
| POST /lecturas | Ingresar una lectura |
| GET /magnitudes/{magnitud} | Valor consolidado, utilizable, canales [{canal, estado, motivo}], avisos |
| GET /estimaciones/actual | Por tipo: probabilidad, nivel, factores |
| GET /alertas?estado= | Lista de alertas |
| POST /alertas/{id}/reconocimiento | Reconocer |
| POST /maniobras · DELETE /maniobras/{id} | Declarar y finalizar |
| GET /config/umbrales · PUT /config/umbrales | Ver vigentes y solicitar cambio |
| POST /config/umbrales/{id}/aprobacion | Aprobar |
| GET /version | Versión de software (3.1.0) y de modelo |
| POST /qa/estimaciones | **Solo perfil qa.** Inyecta una estimación |
| POST /qa/reloj `{ "segundos": n }` | **Solo perfil qa.** Avanza el reloj y evalúa escalamiento y expiración en el momento |
| POST /qa/reset | **Solo perfil qa.** Limpia la base y pone el reloj en T0 |

Los controladores de `web/qa` llevan `@Profile("qa")`. Con el perfil `prod` deben dar 404 (CP-36).

## Panel web (para Selenium)

Página `/panel` con Thymeleaf. Sin JavaScript complejo.
- Tabla de alertas activas: tipo, nivel, estado, destinatarios y un botón **Reconocer** por fila
  (reconocer en 2 interacciones o menos).
- Bloque con el estado de canales de cada magnitud y sus avisos.
- Todos los elementos que se usen en tests llevan `data-test="..."`.
- Modo pausa (`make ui-demo`, `-Dpausa=true`, solo Chrome con ventana): al terminar cada prueba el
  navegador queda abierto con una franja verde/roja y el SISTEMA sigue levantado; cerrar la ventana
  continúa. La espera es un `FluentWait` sobre las ventanas abiertas (sin `Thread.sleep`), tope 10 min.
- Login propio en `/login` (con `data-test`). Botón **Actualizar** en vez de meta refresh: HtmlUnit
  sigue los meta refresh y puede quedar en bucle.
- Recarga simple (meta refresh cada 5 s o botón Actualizar). En los tests se espera por condición
  (WebDriverWait), nunca con sleep.

## Casos de prueba (fuente: docs/Matriz_Trazabilidad_ATALAYA.xlsx)

Cada caso es al menos un test con `@DisplayName("CP-xx · descripción")`. Precondición "Base":
perfil qa, H2 limpia, reloj en T0, sin maniobra, umbrales 40/75, usuario con el rol indicado.

| ID | Qué verifica | Dónde |
|---|---|---|
| CP-01 | Temperatura 19,9 / 20 / 350 / 350,1 | dominio + api + Postman |
| CP-02 | Límites de las otras 4 magnitudes (16 valores) | dominio (@ParameterizedTest) |
| CP-03 | Mensajes inválidos → 400 | api + Postman |
| CP-04 | Duplicada: un solo registro en H2 | integracion (caja gris) |
| CP-05 | Fuera de orden no pisa el valor | integracion |
| CP-06 | 3 canales coherentes | dominio |
| CP-07 | C a 306,64 °C → NO_CONFIABLE por DISCREPANCIA | dominio + Postman + BDD |
| CP-08 | C a 306,60 °C (2,00 %) → CONFIABLE | dominio |
| CP-09 | 10,0 s confiable / 10,1 s SIN_ACTUALIZACION | dominio (reloj) |
| CP-10 | C = 351 → FUERA_DE_RANGO | dominio |
| CP-11 | Menos de 2 confiables → no utilizable + aviso | integracion + Postman |
| CP-12 | Recuperación con 3 lecturas válidas | dominio |
| CP-13 | Niveles 0/14,9/15/39,9/40/74,9/75/100 | dominio + Postman |
| CP-14 | Probabilidad −0,1 / 100,1 / nula rechazada | dominio + api |
| CP-15 | 3 factores ordenados desde 15 % | dominio |
| CP-16 | Con umbrales 30/65: 29,9/30/64,9/65 | dominio |
| CP-17 | Caminos básicos de ClasificadorNivel (−1, 80, 50, 20, 5, 100,1) | dominio (caja blanca) |
| CP-18 | Canales y destinatarios por nivel | integracion + Postman |
| CP-19 | Repetición a 179 s no, a 180 s sí | dominio (reloj) |
| CP-20 | Sube de nivel dentro de los 3 min → se emite | dominio |
| CP-21 | Crítico: 44 s no, 45 s escala, 46 s no duplica | dominio + BDD |
| CP-22 | Alto: 89 / 90 / 91 s | dominio |
| CP-23 | Reconocer a los 44 s evita escalar; reconocimiento guardado | integracion (caja gris) + Postman |
| CP-24 | Reconocer una RECONOCIDA → 409 | api |
| CP-25 | 2 reconocimientos en paralelo × 20 → uno solo efectivo | integracion (ExecutorService + CountDownLatch) |
| CP-26 | Decisiones de ServicioEscalamiento | dominio (caja blanca) |
| CP-27 | 10 estimaciones dentro de la banda → 1 INFORMATIVA | integracion + BDD + Postman |
| CP-28 | Banda 70 a 90: 70 / 69,9 / 90 / 90,1 | dominio |
| CP-29 | Maniobra + canal no confiable → aviso aparte | integracion |
| CP-30 | Expira a 30:00 | dominio (reloj) |
| CP-31 | Pairwise: 10 filas de `datos/pairwise.csv` | integracion (@CsvFileSource) |
| CP-32 | Límites de umbrales (alto 29,9/30/50/50,1; crítico 64,9/65/100/100,1) | dominio + api + Postman |
| CP-33 | Doble aprobación | integracion + BDD |
| CP-34 | Sin credenciales → 401 | api + Postman |
| CP-35 | Matriz de permisos completa | api (@ParameterizedTest) |
| CP-36 | Endpoints qa → 404 con perfil prod | integracion (@ActiveProfiles("prod")) |

Selenium, en `ui/` (no son CP del catálogo original; se suman como UI-xx):
- UI-01: login como operador, ver la alerta ALTO y reconocerla en ≤ 2 clics.
- UI-02: el valor y el nivel del panel coinciden con GET /estimaciones/actual.
- UI-03: el panel muestra el aviso `CANAL_EXCLUIDO`.

### Pairwise (`src/test/resources/datos/pairwise.csv`)

Columnas: `fila,maniobra,canales,rol,reconocimiento,nivel` (el esperado se deriva en el test con las reglas de abajo).
Maniobra: NINGUNA, EXPIRADA, ACTIVA. Canales: 3_CONFIABLES, 1_NO_CONFIABLE, MENOS_DE_2.
Rol: OPERADOR_REACTOR, JEFE_TURNO, SUPERVISOR_SO. Reconocimiento: ANTES, DESPUES, NUNCA. Nivel: ALTO, CRITICO.

```
fila,maniobra,canales,rol,reconocimiento,nivel
1,NINGUNA,3_CONFIABLES,OPERADOR_REACTOR,ANTES,ALTO
2,NINGUNA,1_NO_CONFIABLE,JEFE_TURNO,DESPUES,ALTO
3,NINGUNA,MENOS_DE_2,SUPERVISOR_SO,NUNCA,ALTO
4,EXPIRADA,3_CONFIABLES,JEFE_TURNO,NUNCA,ALTO
5,EXPIRADA,1_NO_CONFIABLE,SUPERVISOR_SO,ANTES,ALTO
6,EXPIRADA,MENOS_DE_2,OPERADOR_REACTOR,DESPUES,CRITICO
7,ACTIVA,3_CONFIABLES,SUPERVISOR_SO,DESPUES,ALTO
8,ACTIVA,1_NO_CONFIABLE,OPERADOR_REACTOR,NUNCA,CRITICO
9,ACTIVA,MENOS_DE_2,JEFE_TURNO,ANTES,CRITICO
10,NINGUNA,3_CONFIABLES,SUPERVISOR_SO,ANTES,CRITICO
```
En maniobra ACTIVA, los niveles ALTO o CRITICO corresponden a un valor de potencia **fuera** de la banda.
"ANTES" = 30 s después de emitida; "DESPUES" = pasado el plazo de escalamiento (45 s o 90 s) + 5 s.
Esperado: escala o no según el plazo, estado final RECONOCIDA o ESCALADA, y aviso de canal cuando corresponda.

## BDD (Cucumber, en español)

`src/test/resources/features/` con `# language: es`. Mínimo 4 features: `redundancia.feature` (CP-07),
`escalamiento.feature` (CP-21, con Esquema del escenario 44/45 s), `maniobras.feature` (CP-27) y
`umbrales.feature` (CP-33). Los steps viven en `bdd/` y usan el contexto de Spring con el perfil qa.
Cada escenario lleva un comentario con el ID del caso.

Reporte: en `src/test/resources/junit-platform.properties`,
`cucumber.plugin=html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json`
(sin `pretty`: el resumen de consola ya muestra cada escenario).

## TDD (con evidencia)

Se aplica a `ValidadorRedundancia` y `ServicioEscalamiento`. Para cada regla, un commit por etapa:
```
red: CP-09 canal sin actualizar más de 10 s
green: CP-09 canal sin actualizar más de 10 s
refactor: extrae evaluación de antigüedad
```
En cada `red` y `green` se guarda la salida de `mvn -Dtest=... test` en
`docs/evidencias/tdd/<clase>-<etapa>-<n>.txt`. **No escribir la implementación antes que el test en
esas dos clases**: si se pide ayuda, primero el test que falla y después el mínimo código para que pase.

## Mutation testing (PIT)

En el `pom.xml`: `targetClasses` = `ar.unsam.vv.atalaya.dominio.*` y `targetTests` = `ar.unsam.vv.atalaya.dominio.*`
(solo unitarias, para que sea rápido). Sin `mutationThreshold`, para no romper el build.
Mutadores: `DEFAULTS` + `INLINE_CONSTS`. Los mutadores por defecto no cambian constantes; sin
`INLINE_CONSTS` no aparecen los mutantes "45 s pasa a 46 s" ni "30 % pasa a 29 %" del informe (10.2).
Para que `INLINE_CONSTS` los alcance, los límites van como literales numéricos (`45`, `BigDecimal.valueOf(30)`),
no como `String` (`new BigDecimal("30")`), y **nunca en `static final`** (PIT no muta inicializadores
estáticos). Usar campos de instancia (`private final Duration plazo = Duration.ofSeconds(45)`) o el literal
en el lugar; un `private final int x = 3` tampoco sirve porque javac lo inlinea. Los mutantes
sobrevivientes se analizan: si falta un caso se agrega; si el mutante es equivalente se documenta en
`docs/pit-analisis.md`.

## Postman

`postman/ATALAYA.postman_collection.json` con una carpeta por requerimiento y una request por caso
marcado con Postman. Cada request lleva un pre-request que llama a `/qa/reset` cuando el caso lo
necesita y tests `pm.test` sobre el código HTTP y el cuerpo. El entorno `qa.postman_environment.json`
tiene `baseUrl` y usuario y contraseña por rol, y `alertaId` y `solicitudId` se cargan desde respuestas.
Las capturas de ejecución manual van a `docs/evidencias/postman/`.

Ejecución: `make postman` (scripts/postman.sh) con newman local (`package.json`, versión fija en
`package-lock.json`, se instala con `make instalar`). Si la app no está en el 8080 la levanta con el
perfil qa y la apaga al final. Primera corrida (31/10): 97 requests, 156 aserciones, 0 fallos.
Node/npm se usan solo para newman: nada del SISTEMA ni de las pruebas depende de Node.

Generador: `postman/generar_coleccion.py` (regenerar en vez de editar el JSON a mano). El caso CP-27
repite la inyección ×10 con `postman.setNextRequest`: correrlo con el Collection Runner o `newman`.

## Defectos

- Los defectos se reportan en Jira (fuera del repo). En el código, citar la clave en el `@DisplayName`
  del test que lo evidencia.
- **Tests que fallan a propósito**: si al entregar un defecto sigue abierto, el test que lo evidencia
  queda con `@DisplayName("CP-xx · ... [defecto ATA-n]")`. Solo con un defecto real y reportado.
- Si el equipo decide sembrar defectos para la demostración, se marcan como sembrados en Jira y en el
  informe. No presentarlos como hallazgos espontáneos.

## Convenciones de código

- Nombres de dominio en español, como en el enunciado (Lectura, Canal, Magnitud, Alerta, Maniobra, Umbrales).
- Comparaciones numéricas de reglas con `BigDecimal`.
- Métodos chicos; sin abstracciones que el alcance no necesite.
- Cada clase de test: Javadoc con el propósito y los requerimientos que cubre.
- Nada de `@Disabled` sin una razón escrita.
- Antes de dar algo por terminado: `mvn verify` en verde.

## Pendientes del informe (TP_ATALAYA_QA_v6.pdf)

El informe es del equipo; Claude no lo edita. Correcciones a hacer en la próxima versión:
- Sección 2, tabla ISO 25010 (Usabilidad) y sección 12: el panel web y Selenium ya no están fuera del
  alcance (pedido del profesor). Sumar UI-01 a UI-03 a casos, trazabilidad y planilla.
- 7.3: el código de `ClasificadorNivel` usa `BigDecimal` y `ProbabilidadInvalidaException`, no `double`.
- 10.2: aclarar que los mutantes de constantes salen de `INLINE_CONSTS`.
- 6: el entorno de desarrollo no tiene por qué ser VS Code (hay integrantes con IntelliJ).

## Qué no hacer

- No implementar RF-04/05, RF-10 ni RF-11, ni un modelo de IA.
- No agregar frameworks o herramientas de testing fuera de la lista.
- No usar `Thread.sleep` ni esperas fijas.
- No dejar endpoints de QA sin `@Profile("qa")`.
- No modificar los valores límite de esta guía sin avisar: están atados al informe y a la planilla.
