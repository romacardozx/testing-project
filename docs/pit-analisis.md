# Análisis de mutantes sobrevivientes (PIT)

Comando: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` (PIT no recompila: sin
`test-compile` usa las clases de test anteriores). Mutadores `DEFAULTS` + `INLINE_CONSTS` sobre
`ar.unsam.vv.atalaya.dominio`.

## 31/10/2026 · RF-01/02 y RF-03

Primera corrida: 76 mutantes, 65 muertos (86 %).

| Clase y línea | Mutante | Análisis | Acción |
|---|---|---|---|
| ValidadorRedundancia, mediana | `% 2 == 1` reemplazado (constante, operador, condición) | Ningún caso tenía tres valores distintos donde la mediana difiriera del promedio de los dos menores | Caso nuevo "con tres valores distintos la mediana es el del medio" |
| ValidadorRedundancia, mediana | índice `medio - 1` → `medio - 0` | Ningún caso tenía exactamente dos canales con valores distintos | Caso nuevo "con solo dos canales con valor la mediana es su promedio" |
| ValidadorRedundancia, promedio | escala 2 → 3 | Los casos comparaban con `isEqualByComparingTo`, que ignora la escala | Caso nuevo "el valor consolidado se redondea a 2 decimales" (300,67) |
| Magnitud, `desde` y `unidad` | retorno vacío / constante | Sin cobertura en el paquete dominio (solo se usaba desde la API) | `MagnitudTest` |
| SenalCanal.sinDatos | `ultimaFueraDeRango` false → true | **Equivalente.** Un canal sin datos tiene `ultimaRecepcion = null` y la regla 1 (SIN_ACTUALIZACION) se evalúa antes que la regla 2: el flag nunca se lee | Se documenta, no se agrega caso |

Corrida final: 76 mutantes, 75 muertos (99 %). Sobrevive solo el equivalente de `SenalCanal`.

## 31/10/2026 · RF-06 y RF-07/08

Primera corrida: 130 mutantes, 128 muertos (98 %).

| Clase y línea | Mutante | Análisis | Acción |
|---|---|---|---|
| ClasificadorNivel, `cantidadFactores = 3` | 3 → 4 | **Equivalente por cómo compila Java**: un `final int` con valor constante se inlinea en `limit(3)`; el mutante cambia el campo pero nadie lo lee | Se usa el literal en `limit(3)`; el mutante nuevo lo mata CP-15 |
| ValidadorRedundancia, constantes `static final` | — | PIT no muta inicializadores estáticos: el mutante "10 s → 11 s" (CP-09) ni siquiera se generaba | Constantes pasadas a campos de instancia o literales; ahora se generan y mueren |

Mutantes de la sección 10.2 del informe, todos generados y muertos:

| Mutante | Dónde | Lo mata |
|---|---|---|
| `>=` → `>` en 15 % | ClasificadorNivel | CP-13 |
| 15 → 16 | ClasificadorNivel | CP-13 |
| `>` → `>=` en el 2 % | ValidadorRedundancia | CP-08 |
| 45 → 46 | ServicioEscalamiento | CP-21 |
| 90 → 91 | ServicioEscalamiento | CP-22 |
| 180 → 181 | PoliticaRepeticion | CP-19 |
| se elimina la condición "ya reconocida" | ServicioEscalamiento.reconocer | CP-24 |

El mutante "30 % → 29 % en ValidadorUmbrales" queda para RF-12.

Corrida final: 131 mutantes, 130 muertos (99 %). Sobrevive solo el equivalente de `SenalCanal`.
JaCoCo: ClasificadorNivel y ServicioEscalamiento con 100 % de líneas y ramas (CP-17, CP-26).

## 31/10/2026 · RF-09 y RF-12

Corrida: 162 mutantes, 159 muertos (98 %).

| Clase y línea | Mutante | Análisis | Acción |
|---|---|---|---|
| ValidadorUmbrales, `alto >= critico` | borde y condición eliminada (2) | **Equivalentes**: la regla es inalcanzable con los rangos de RF-12 (alto ≤ 50 < 65 ≤ crítico). Ningún dato válido para las dos primeras reglas puede violarla | Se documenta. Hallazgo para el informe: la regla del requerimiento es redundante |
| UmbralesInvalidosException.campo | retorno "" | Sin cobertura: AssertJ `extracting("campo")` leía el campo por reflexión, no el método | El test usa `campo()` |

Mutante "30 % → 31 %" de ValidadorUmbrales (sección 10.2, ahí figura como 30 → 29): generado y muerto por CP-32.
