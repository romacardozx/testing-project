# Registro de defectos · ATALAYA 3.1.0 · Grupo E

No tuvimos Jira, así que llevamos los defectos en este archivo con los mismos campos que pide la
sección 11 del informe. Las claves (ATA-n) son las que citan la planilla, el informe y los nombres de
los tests.

Severidad: **crítica** si puede llevar al operador a una mala decisión, **alta** si demora o duplica
una alerta, **media** si es un error sin efecto directo y **baja** si es cosmético.
Flujo: Nuevo → Confirmado → En corrección → Corregido → Verificado → Cerrado.

## Resumen

| Clave | Tipo | Resumen | Caso | Sev. · Prio. | Estado |
|---|---|---|---|---|---|
| ATA-1 | Especificación | RF-03 no define contra qué ni sobre qué rango se mide el 2 % (A1, A2) | CP-07, CP-08 | Alta · Alta | Confirmado |
| ATA-2 | Especificación | RF-03 no dice cuántas lecturas buenas necesita un canal para recuperarse (A3) | CP-12 | Media · Media | Confirmado |
| ATA-3 | Especificación | RF-07 no define qué significa que una alerta "empeore" (A4) | CP-20 | Alta · Alta | Confirmado |
| ATA-4 | Especificación | RF-08 no dice qué pasa con dos reconocimientos simultáneos (A5) | CP-25 | Media · Media | Confirmado |
| ATA-5 | Especificación | RF-09 no dice qué pasa con una maniobra declarada que nunca se ejecuta (A6) | CP-30 | Alta · Media | Confirmado |
| ATA-6 | Especificación | RF-06 no aclara dónde empieza ATENCION si se baja el umbral alto (A7) | CP-16 | Media · Media | Confirmado |
| ATA-7 | Especificación | RF-12 no fija un máximo para el umbral crítico (A8) | CP-32 | Media · Baja | Confirmado |
| ATA-8 | Especificación | RF-03 regla 1: no queda claro si "sin actualizar" mira la última lectura recibida o la aceptada | CP-09, CP-10 | Alta · Alta | Confirmado |
| ATA-9 | Especificación | RF-07 no dice qué hacer con un nivel menor dentro de los 3 min | CP-19, CP-20 | Media · Media | Confirmado |
| ATA-10 | Especificación | RF-09 no dice si puede haber más de una maniobra activa | CP-39 | Media · Media | Confirmado |
| ATA-11 | Especificación | RF-12: la regla "alto menor que crítico" nunca puede fallar con los rangos de A8 | CP-32 | Baja · Baja | Confirmado |
| ATA-12 | Especificación | RF-12: "aprueba el mismo usuario" no se puede dar por la API con un rol por usuario | CP-33 | Baja · Baja | Confirmado |
| ATA-13 | Diseño de pruebas | CP-34 pide 401 en todos los endpoints, pero los de QA no piden credenciales | CP-34, CP-36 | Baja · Media | Cerrado |
| ATA-14 | Diseño de pruebas | La planilla usa la ruta /magnitudes/temperatura en CP-06 | CP-06 | Baja · Media | Cerrado |
| ATA-15 | Diseño de pruebas | El esperado de CP-09 habla solo del canal C | CP-09 | Baja · Media | Cerrado |
| ATA-16 | SISTEMA | Sin credenciales, la API responde 302 al login en vez de 401 | CP-34, CP-35 | Alta · Alta | Cerrado |
| ATA-17 | SISTEMA | La base H2 se comparte entre contextos y CP-18 falla a veces | CP-18 | Media · Alta | Cerrado |
| ATA-18 | SISTEMA | El panel muestra los estados de los tres canales pegados en un renglón | UI-03 | Baja · Baja | Cerrado |

Defectos del SISTEMA: 3, todos cerrados. Defectos de especificación: 12, confirmados y resueltos con
una decisión del equipo (sección 5 del informe). No quedan defectos críticos abiertos.

## Defectos del SISTEMA

### ATA-16 · Sin credenciales, la API responde 302 al login en vez de 401

| Campo | Contenido |
|---|---|
| Tipo | Defecto del SISTEMA (seguridad) |
| Versión | 3.1.0 |
| Requerimiento y caso | RNF seguridad · CP-34, CP-35 |
| Detectado | 31/10/2026, corriendo la colección de Postman contra el SISTEMA levantado. Reportó N. Beda |
| Pasos | 1. Levantar el SISTEMA con el perfil qa. 2. `GET /api/v1/alertas` sin credenciales. 3. Repetir con el usuario auditor contra `POST /api/v1/lecturas` |
| Esperado | 401 en el paso 2 y 403 en el paso 3, con el error en JSON |
| Observado | 302 con `Location: /login` en los dos casos. Un cliente que sigue redirecciones recibe el HTML del login con 200 |
| Causa | La API respondía con `sendError`. El servidor reenvía el error a `/error`, que caía en la cadena de seguridad del panel, y esa cadena redirige al login. Con MockMvc no se ve porque no hace ese reenvío: CP-34 y CP-35 pasaban |
| Corrección | La API escribe el 401 y el 403 directamente como JSON, y `/error` queda permitido |
| Evidencia | `SeguridadServidorRealTest` (contra Tomcat real, sin seguir redirecciones), `docs/evidencias/postman/newman.txt` |
| Severidad y prioridad | Alta · Alta |
| Estado | Cerrado el 31/10/2026. La regresión corre en `mvn verify` |

### ATA-17 · La base H2 se comparte entre contextos y CP-18 falla a veces

| Campo | Contenido |
|---|---|
| Tipo | Defecto del SISTEMA (configuración) |
| Versión | 3.1.0 |
| Requerimiento y caso | RF-07/08 · CP-18 |
| Detectado | 31/10/2026, corriendo la suite completa varias veces. Reportó V. Geronzi |
| Pasos | 1. `mvn verify` con la URL `jdbc:h2:mem:atalaya` fija. 2. Repetir. CP-18 falla en algunas corridas según el orden de las clases |
| Esperado | Cada contexto de Spring (qa y prod) con su propia base |
| Observado | Los dos contextos usaban la misma base en memoria. El tick de escalamiento de prod, con reloj real, escalaba las alertas que las pruebas qa creaban en T0, y CP-18 encontraba alertas ESCALADA donde esperaba EMITIDA |
| Corrección | `spring.datasource.generate-unique-name: true` en `application.yml` |
| Evidencia | Test "CP-18 · la base H2 es propia de cada contexto…" en `BaseQaIntegracionTest` |
| Severidad y prioridad | Media · Alta (no afecta al operador, pero un test que falla a veces deja de servir) |
| Estado | Cerrado el 31/10/2026 |

### ATA-18 · El panel muestra los estados de los tres canales pegados en un renglón

| Campo | Contenido |
|---|---|
| Tipo | Defecto del SISTEMA (interfaz) |
| Versión | 3.1.0 |
| Requerimiento y caso | RF-03 · UI-03 |
| Detectado | 09/11/2026, revisando las capturas de Selenium. Reportó N. Beda |
| Pasos | 1. Enviar temperatura A = B = 300,00 y C = 306,64. 2. Entrar al panel como operador. 3. Mirar "Estado de canales" |
| Esperado | Un renglón por canal: "A: CONFIABLE", "B: CONFIABLE", "C: NO_CONFIABLE (DISCREPANCIA)" |
| Observado | "A: CONFIABLEB: CONFIABLEC: NO_CONFIABLE (DISCREPANCIA)". UI-03 pasaba igual porque leía cada canal por separado |
| Corrección | Los estados pasan a ser una lista (un `li` por canal) en `panel.html` |
| Evidencia | Test "UI-03 · cada canal se muestra en su propio renglón" en `PanelUiTest`; rojo y verde en `docs/evidencias/defectos/ATA-18-red.txt` y `ATA-18-green.txt`; captura `docs/evidencias/selenium/UI-03_panelMuestraAvisoDeCanal.png` |
| Severidad y prioridad | Baja · Baja (cosmético; los datos son correctos) |
| Estado | Cerrado el 09/11/2026 |

## Defectos de especificación

Salieron de la revisión de requerimientos (30/10) y de la implementación (31/10). Ninguno tiene
corrección en el código: el SISTEMA aplica la decisión del equipo y los casos la verifican.

### ATA-1 · RF-03 no define contra qué ni sobre qué rango se mide el 2 % (A1, A2)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-03 · CP-07, CP-08 |
| Descripción | El requerimiento dice "discrepa más del 2 % del rango respecto de los otros". Con tres canales no queda claro si se compara contra la mediana, el promedio o cada par, ni si el rango es el físico o el del instrumento |
| Impacto | Dos implementaciones correctas según el texto marcan canales distintos como no confiables, y eso cambia la estimación |
| Decisión | Contra la mediana de los canales con valor, y sobre el rango físico de RF-01 (330 °C → 6,6 °C) |
| Severidad y prioridad | Alta · Alta |
| Estado | Confirmado; se pidió aclaración a la cátedra. Mientras tanto se implementa la decisión A1/A2 |

### ATA-2 · RF-03 no dice cuántas lecturas buenas necesita un canal para recuperarse (A3)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-03 · CP-12 |
| Descripción | El texto dice que el canal "vuelve a ser confiable" pero no cuándo. Si vuelve con la primera lectura buena, un canal que oscila entra y sale de la consolidación todo el tiempo |
| Decisión | 3 lecturas seguidas que cumplen las reglas 1 a 3. Cualquier lectura que no las cumple reinicia la cuenta |
| Severidad y prioridad | Media · Media |
| Estado | Confirmado; se aplica la decisión A3 |

### ATA-3 · RF-07 no define qué significa que una alerta "empeore" (A4)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-07 · CP-20 |
| Descripción | "No se repite durante 3 min salvo que empeore". Empeorar puede ser subir de nivel o subir la probabilidad dentro del mismo nivel |
| Impacto | Si es por probabilidad, una estimación que sube de 41 % a 42 % repite la alerta y vuelve el exceso de alertas que la regla quiere evitar |
| Decisión | Empeorar es pasar a un nivel mayor. Mismo nivel con más probabilidad no se emite |
| Severidad y prioridad | Alta · Alta |
| Estado | Confirmado; se aplica la decisión A4 |

### ATA-4 · RF-08 no dice qué pasa con dos reconocimientos simultáneos (A5)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-08 · CP-25 |
| Descripción | Operador y jefe de turno pueden reconocer la misma alerta al mismo tiempo. El requerimiento no dice si vale uno, los dos o el último |
| Decisión | Vale el primero; el segundo recibe 409. Se implementa con bloqueo optimista (`@Version`) |
| Severidad y prioridad | Media · Media |
| Estado | Confirmado; se aplica la decisión A5 |

### ATA-5 · RF-09 no dice qué pasa con una maniobra declarada que nunca se ejecuta (A6)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-09 · CP-30 |
| Descripción | Si el jefe declara una maniobra y no la finaliza, las variaciones dentro de la banda se seguirían agrupando para siempre |
| Impacto | Un desvío real dentro de la banda puede quedar escondido en la alerta informativa |
| Decisión | La maniobra expira a los 30 min (`duracionMaximaMin`, por defecto 30). A 29:59 se agrupa; a 30:00 no |
| Severidad y prioridad | Alta · Media |
| Estado | Confirmado; se aplica la decisión A6 |

### ATA-6 · RF-06 no aclara dónde empieza ATENCION si se baja el umbral alto (A7)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-06, RF-12 · CP-16 |
| Descripción | Con los umbrales por defecto ATENCION va de 15 % a 40 %. Si el umbral alto pasa a 30 %, no se dice si ATENCION queda de 15 % a 30 % o se corre |
| Decisión | ATENCION va siempre de 15 % al umbral alto vigente |
| Severidad y prioridad | Media · Media |
| Estado | Confirmado; se aplica la decisión A7 |

### ATA-7 · RF-12 no fija un máximo para el umbral crítico (A8)

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-12 · CP-32 |
| Descripción | El umbral alto tiene rango (30 a 50 %) y el crítico solo mínimo (65 %). Un crítico de 120 % dejaría el nivel CRITICO inalcanzable |
| Decisión | Crítico entre 65 % y 100 %, inclusive |
| Severidad y prioridad | Media · Baja |
| Estado | Confirmado; se aplica la decisión A8 |

### ATA-8 · RF-03 regla 1: ¿última lectura recibida o aceptada?

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-03 · CP-09, CP-10 |
| Descripción | La regla dice "sin lectura aceptada hace más de 10 s". Si se toma al pie de la letra, un canal que manda 351 °C sin parar queda como SIN_ACTUALIZACION y nunca como FUERA_DE_RANGO, y CP-10 no se puede cumplir |
| Decisión | La regla 1 mira la última lectura recibida, aceptada o descartada por rango. Las DUPLICADA y FUERA_DE_ORDEN no cuentan. Un canal que nunca envió nada está SIN_ACTUALIZACION |
| Severidad y prioridad | Alta · Alta |
| Estado | Confirmado; se aplica la decisión (sección 5 del informe) |

### ATA-9 · RF-07 no dice qué hacer con un nivel menor dentro de los 3 min

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-07 · CP-19, CP-20 |
| Descripción | Hay regla para el mismo nivel (no se repite) y para uno mayor (se emite), pero no para uno menor. Ejemplo: CRITICO en T0 y ALTO en T0 + 60 s |
| Decisión | No se emite: bajar de nivel no es empeorar (ATA-3) |
| Severidad y prioridad | Media · Media |
| Estado | Confirmado; se aplica la decisión |

### ATA-10 · RF-09 no dice si puede haber más de una maniobra activa

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-09 · CP-39 |
| Descripción | Con dos maniobras activas sobre magnitudes distintas no queda claro contra cuál se evalúa cada estimación |
| Decisión | Una sola maniobra ACTIVA. Declarar otra devuelve 409 `MANIOBRA_ACTIVA`; finalizar una que no está activa devuelve 409 `MANIOBRA_NO_ACTIVA` |
| Severidad y prioridad | Media · Media |
| Estado | Confirmado; se aplica la decisión |

### ATA-11 · RF-12: la regla "alto menor que crítico" nunca puede fallar

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-12 · CP-32 |
| Descripción | Con alto entre 30 y 50 y crítico entre 65 y 100, siempre alto ≤ 50 < 65 ≤ crítico. La regla no se puede violar |
| Impacto | Ninguno sobre el operador. PIT deja dos mutantes vivos en esa línea porque son equivalentes (`docs/pit-analisis.md`) |
| Decisión | La regla queda en el código porque la pide el requerimiento |
| Severidad y prioridad | Baja · Baja |
| Estado | Confirmado |

### ATA-12 · RF-12: "aprueba el mismo usuario" no se puede dar por la API

| Campo | Contenido |
|---|---|
| Requerimiento y caso | RF-12 · CP-33 |
| Descripción | Solo el supervisor de SO solicita y solo el jefe de turno aprueba. Como cada usuario tiene un rol, el supervisor que intenta aprobar su propia solicitud recibe 403 por rol antes de llegar a la regla del mismo usuario (409) |
| Decisión | El 409 se prueba llamando al servicio (`UmbralesIntegracionTest`, `ServicioUmbralesTest`). Por la API se verifica el 403 |
| Severidad y prioridad | Baja · Baja |
| Estado | Confirmado |

## Defectos del diseño de pruebas

Errores en la planilla o en los casos, no en el SISTEMA. Se corrigieron en la planilla.

### ATA-13 · CP-34 pide 401 en todos los endpoints, pero los de QA no piden credenciales

| Campo | Contenido |
|---|---|
| Caso | CP-34, CP-36 |
| Descripción | Los endpoints `/api/v1/qa/**` no piden credenciales a propósito: así, con el perfil prod responden 404 (no existen) y no 401 (CP-36). CP-34 decía "401 en todos" |
| Corrección | CP-34 cubre todos los endpoints salvo los de QA |
| Severidad y prioridad | Baja · Media |
| Estado | Cerrado el 31/10/2026 |

### ATA-14 · La planilla usa la ruta /magnitudes/temperatura en CP-06

| Campo | Contenido |
|---|---|
| Caso | CP-06 |
| Descripción | La magnitud se llama `TEMPERATURA_PRIMARIO`. `GET /api/v1/magnitudes/temperatura` devuelve 404 |
| Corrección | La planilla usa `/api/v1/magnitudes/TEMPERATURA_PRIMARIO` |
| Severidad y prioridad | Baja · Media |
| Estado | Cerrado el 09/11/2026 |

### ATA-15 · El esperado de CP-09 habla solo del canal C

| Campo | Contenido |
|---|---|
| Caso | CP-09 |
| Descripción | Si se avanza el reloj sin enviar lecturas, a los 10,1 s quedan sin actualizar los tres canales, no solo C. El caso no decía qué hacían A y B |
| Corrección | Los tres canales se envían en T0 y se verifica que a los 10,0 s siguen confiables y a los 10,1 s los tres pasan a SIN_ACTUALIZACION |
| Severidad y prioridad | Baja · Media |
| Estado | Cerrado el 09/11/2026 |
