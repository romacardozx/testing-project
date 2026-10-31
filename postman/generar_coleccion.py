"""Genera la colección y el entorno de Postman de ATALAYA (casos marcados con Postman en la planilla).

Uso: python3 postman/generar_coleccion.py postman
Si se cambia un caso, regenerar en vez de editar el JSON a mano.
"""
import json, sys, uuid

destino = sys.argv[1]
RESET = ["// Precondición Base: H2 limpia y reloj en T0",
         "pm.sendRequest({ url: pm.environment.get('baseUrl') + '/api/v1/qa/reset', method: 'POST' },",
         "    function (err) { if (err) { throw err; } });"]

def auth(usuario):
    if usuario is None:
        return {"type": "noauth"}
    return {"type": "basic", "basic": [
        {"key": "username", "value": "{{usr_%s}}" % usuario, "type": "string"},
        {"key": "password", "value": "{{pwd_%s}}" % usuario, "type": "string"}]}

def req(nombre, metodo, ruta, cuerpo=None, usuario=None, tests=(), pre=(), reset=False, descripcion=None):
    eventos = []
    pre = list(pre)
    if reset:
        pre = RESET + pre
    if pre:
        eventos.append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre}})
    if tests:
        eventos.append({"listen": "test", "script": {"type": "text/javascript", "exec": list(tests)}})
    partes = ruta.strip("/").split("?")[0].split("/")
    r = {"method": metodo, "header": [], "auth": auth(usuario),
         "url": {"raw": "{{baseUrl}}" + ruta, "host": ["{{baseUrl}}"], "path": partes}}
    if "?" in ruta:
        r["url"]["query"] = [{"key": k, "value": v} for k, v in (p.split("=") for p in ruta.split("?")[1].split("&"))]
    if cuerpo is not None:
        r["header"] = [{"key": "Content-Type", "value": "application/json"}]
        r["body"] = {"mode": "raw", "raw": cuerpo, "options": {"raw": {"language": "json"}}}
    if descripcion:
        r["description"] = descripcion
    return {"name": nombre, "event": eventos, "request": r}

def carpeta(nombre, items, descripcion=None):
    c = {"name": nombre, "item": items}
    if descripcion:
        c["description"] = descripcion
    return c

def codigo(n):
    return "pm.test('HTTP %d', function () { pm.response.to.have.status(%d); });" % (n, n)

def campo(expr, valor, titulo=None):
    titulo = titulo or "%s = %s" % (expr, valor)
    return "pm.test(%s, function () { pm.expect(pm.response.json()%s).to.eql(%s); });" % (
        json.dumps(titulo), expr, json.dumps(valor))

def lectura(magnitud, canal, valor, secuencia):
    return ('{"magnitud": "%s", "canal": "%s", "valor": %s, "secuencia": %d, '
            '"timestampOrigen": "2026-01-01T00:00:00Z"}') % (magnitud, canal, valor, secuencia)

def post_lectura(nombre, magnitud, canal, valor, secuencia, estado="ACEPTADA", reset=False):
    return req(nombre, "POST", "/api/v1/lecturas", lectura(magnitud, canal, valor, secuencia), "adquisicion",
               [codigo(200), campo(".estado", estado)], reset=reset)

def estimacion(nombre, tipo, prob, nivel=None, guardar=False, reset=False, tests_extra=()):
    tests = [codigo(200)]
    if nivel:
        tests.append(campo(".nivel", nivel))
    if guardar:
        tests.append("pm.environment.set('alertaId', pm.response.json().alertaId);")
    tests += list(tests_extra)
    return req(nombre, "POST", "/api/v1/qa/estimaciones", '{"tipo": "%s", "probabilidad": %s}' % (tipo, prob),
               None, tests, reset=reset)

def reloj(segundos):
    return req("Avanzar reloj %s s" % segundos, "POST", "/api/v1/qa/reloj", '{"segundos": %s}' % segundos, None,
               [codigo(200)])

T = "TEMPERATURA_PRIMARIO"
P = "POTENCIA_TERMICA"

rf01 = carpeta("RF-01/02 · Lecturas", [
    carpeta("CP-01 · Límites de temperatura del primario", [
        post_lectura("19,9 °C → DESCARTADA_RANGO", T, "A", "19.9", 1, "DESCARTADA_RANGO", reset=True),
        post_lectura("20 °C → ACEPTADA", T, "A", "20", 2),
        post_lectura("350 °C → ACEPTADA", T, "A", "350", 3),
        post_lectura("350,1 °C → DESCARTADA_RANGO", T, "A", "350.1", 4, "DESCARTADA_RANGO"),
    ], "Valores límite. Precondición Base."),
    carpeta("CP-03 · Mensajes inválidos", [
        req("Valor nulo → 400", "POST", "/api/v1/lecturas",
            '{"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": null, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}',
            "adquisicion", [codigo(400), campo(".campo", "valor")], reset=True),
        req("Valor no numérico → 400", "POST", "/api/v1/lecturas",
            '{"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": "abc", "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}',
            "adquisicion", [codigo(400), campo(".campo", "valor")]),
        req("Magnitud inexistente → 400", "POST", "/api/v1/lecturas",
            '{"magnitud": "CAUDAL", "canal": "A", "valor": 300, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}',
            "adquisicion", [codigo(400), campo(".campo", "magnitud")]),
        req("Canal D → 400", "POST", "/api/v1/lecturas",
            '{"magnitud": "TEMPERATURA_PRIMARIO", "canal": "D", "valor": 300, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}',
            "adquisicion", [codigo(400), campo(".campo", "canal")]),
        req("JSON malformado → 400", "POST", "/api/v1/lecturas", '{"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", ',
            "adquisicion", [codigo(400)]),
        req("La magnitud no cambió", "GET", "/api/v1/magnitudes/TEMPERATURA_PRIMARIO", None, "operador",
            [codigo(200), campo(".utilizable", False), campo(".valorConsolidado", None)]),
    ], "Partición de equivalencia: una clase inválida por request."),
])

rf03 = carpeta("RF-03 · Redundancia", [
    carpeta("CP-07 · Canal que discrepa 2,01 %", [
        post_lectura("A = 300,00", T, "A", "300.00", 1, reset=True),
        post_lectura("B = 300,00", T, "B", "300.00", 1),
        post_lectura("C = 306,64", T, "C", "306.64", 1),
        req("C NO_CONFIABLE por DISCREPANCIA; valor con A y B", "GET", "/api/v1/magnitudes/TEMPERATURA_PRIMARIO", None,
            "operador", [codigo(200), campo(".canales[2].estado", "NO_CONFIABLE"),
                         campo(".canales[2].motivo", "DISCREPANCIA"), campo(".valorConsolidado", 300),
                         campo(".avisos", ["CANAL_EXCLUIDO:C"])]),
    ]),
    carpeta("CP-11 · Menos de dos canales confiables", [
        post_lectura("A = 300", T, "A", "300", 1, reset=True),
        post_lectura("B = 300", T, "B", "300", 1),
        post_lectura("C = 300", T, "C", "300", 1),
        reloj(5), post_lectura("A = 300 (5 s)", T, "A", "300", 2),
        reloj(5), post_lectura("A = 300 (10 s)", T, "A", "300", 3),
        reloj(1), post_lectura("A = 300 (11 s)", T, "A", "300", 4),
        req("No utilizable y aviso explícito", "GET", "/api/v1/magnitudes/TEMPERATURA_PRIMARIO", None, "operador",
            [codigo(200), campo(".utilizable", False),
             "pm.test('avisos', function () { pm.expect(pm.response.json().avisos).to.include.members(['CANAL_EXCLUIDO:B', 'CANAL_EXCLUIDO:C', 'MAGNITUD_NO_DISPONIBLE']); });"]),
    ], "B y C dejan de actualizar más de 10 s; A sigue enviando."),
])

bordes = [("0", "NORMAL"), ("14.9", "NORMAL"), ("15", "ATENCION"), ("39.9", "ATENCION"),
          ("40", "ALTO"), ("74.9", "ALTO"), ("75", "CRITICO"), ("100", "CRITICO")]
rf06 = carpeta("RF-06 · Niveles", [
    carpeta("CP-13 · Bordes de nivel", [
        estimacion("%s %% → %s" % (p.replace(".", ","), n), "PROCESO", p, n, reset=(i == 0))
        for i, (p, n) in enumerate(bordes)
    ], "Umbrales por defecto 40/75."),
])

rf07 = carpeta("RF-07/08 · Alertas", [
    carpeta("CP-18 · Destinatarios según nivel", [
        estimacion("20 % (ATENCION)", "PROCESO", "20", "ATENCION", reset=True),
        estimacion("50 % (ALTO)", "PROCESO", "50", "ALTO"),
        estimacion("80 % (CRITICO)", "PROCESO", "80", "CRITICO"),
        req("Canales y destinatarios", "GET", "/api/v1/alertas", None, "operador", [
            codigo(200),
            campo("[0].canales", ["VISUAL"]), campo("[0].destinatarios", []), campo("[0].requiereReconocimiento", False),
            campo("[1].destinatarios", ["OPERADOR_REACTOR"]), campo("[1].requiereReconocimiento", True),
            campo("[2].canales", ["VISUAL", "SONORA"]), campo("[2].destinatarios", ["OPERADOR_REACTOR", "JEFE_TURNO"]),
        ]),
    ]),
    carpeta("CP-23 · Reconocimiento antes del plazo", [
        estimacion("Alerta CRITICO en T0", "PROCESO", "80", "CRITICO", guardar=True, reset=True),
        reloj(44),
        req("El operador reconoce a los 44 s", "POST", "/api/v1/alertas/{{alertaId}}/reconocimiento", None, "operador",
            [codigo(200), campo(".estado", "RECONOCIDA")]),
        reloj(16),
        req("A los 60 s sigue RECONOCIDA y sin escalar", "GET", "/api/v1/alertas?estado=RECONOCIDA", None, "operador",
            [codigo(200), campo(".length", 1, "una alerta reconocida"),
             campo("[0].destinatarios", ["OPERADOR_REACTOR", "JEFE_TURNO"])]),
    ], "El registro del reconocimiento (usuario y hora) se verifica en H2 con JUnit (caja gris)."),
])

rf09 = carpeta("RF-09 · Maniobras", [
    carpeta("CP-27 · Variaciones dentro de la banda", [
        post_lectura("Potencia A = 80", P, "A", "80", 1, reset=True),
        post_lectura("Potencia B = 80", P, "B", "80", 1),
        post_lectura("Potencia C = 80", P, "C", "80", 1),
        req("El jefe declara la maniobra (banda 70 a 90)", "POST", "/api/v1/maniobras",
            '{"magnitud": "POTENCIA_TERMICA", "bandaMin": 70, "bandaMax": 90, "duracionMaximaMin": 30}', "jefe",
            [codigo(201), campo(".estado", "ACTIVA"), "pm.environment.set('inyectadas', 0);"]),
        req("Inyectar estimación ALTO dentro de la banda (×10)", "POST", "/api/v1/qa/estimaciones",
            '{"tipo": "PROCESO", "probabilidad": 50}', None, [
                codigo(200),
                "const n = Number(pm.environment.get('inyectadas')) + 1;",
                "pm.environment.set('inyectadas', n);",
                "if (n < 10) { postman.setNextRequest(pm.info.requestName); }"]),
        req("Una sola alerta INFORMATIVA con 10 agrupadas", "GET", "/api/v1/alertas", None, "operador",
            [codigo(200), campo(".length", 1, "una sola alerta"), campo("[0].clase", "INFORMATIVA"),
             campo("[0].agrupadas", 10)]),
    ], "La repetición ×10 usa postman.setNextRequest: correr la carpeta con el Collection Runner o newman."),
])

rf0308 = carpeta("CP-31 · Pairwise (fila 2 como muestra)", [
    post_lectura("Potencia A = 80", P, "A", "80", 1, reset=True),
    post_lectura("Potencia B = 80", P, "B", "80", 1),
    post_lectura("Potencia C = 85 (discrepa)", P, "C", "85", 1),
    estimacion("Alerta ALTO", "PROCESO", "50", "ALTO", guardar=True),
    req("Aviso de canal", "GET", "/api/v1/magnitudes/POTENCIA_TERMICA", None, "operador",
        [codigo(200), campo(".avisos", ["CANAL_EXCLUIDO:C"])]),
    reloj(89),
    req("89 s: EMITIDA", "GET", "/api/v1/alertas", None, "operador", [codigo(200), campo("[0].estado", "EMITIDA")]),
    reloj(1),
    req("90 s: ESCALADA al jefe de turno", "GET", "/api/v1/alertas", None, "operador",
        [codigo(200), campo("[0].estado", "ESCALADA"), campo("[0].destinatarios", ["OPERADOR_REACTOR", "JEFE_TURNO"])]),
    reloj(5),
    req("95 s: el jefe reconoce", "POST", "/api/v1/alertas/{{alertaId}}/reconocimiento", None, "jefe",
        [codigo(200), campo(".estado", "RECONOCIDA")]),
], "Fila 2: maniobra NINGUNA, 1 canal no confiable, reconoce el jefe después del plazo, nivel ALTO. Las 10 filas corren en JUnit (PairwiseIntegracionTest).")

limites = [("29.9", "75", 422), ("30", "75", 201), ("50", "75", 201), ("50.1", "75", 422),
           ("40", "64.9", 422), ("40", "65", 201), ("40", "100", 201), ("40", "100.1", 422)]
rf12 = carpeta("RF-12 · Umbrales", [
    carpeta("CP-32 · Límites de umbrales", [
        req("Alto %s / crítico %s → %d" % (a.replace(".", ","), c.replace(".", ","), n), "PUT", "/api/v1/config/umbrales",
            '{"alto": %s, "critico": %s}' % (a, c), "supervisor",
            [codigo(n)] + ([campo(".estado", "PENDIENTE")] if n == 201 else []), reset=(i == 0))
        for i, (a, c, n) in enumerate(limites)
    ] + [req("Los vigentes no cambiaron (solicitudes PENDIENTE)", "GET", "/api/v1/config/umbrales", None, "operador",
             [codigo(200), campo(".alto", 40), campo(".critico", 75)])]),
])

sin_cred = [("POST", "/api/v1/lecturas"), ("GET", "/api/v1/magnitudes/TEMPERATURA_PRIMARIO"),
            ("GET", "/api/v1/estimaciones/actual"), ("GET", "/api/v1/alertas"),
            ("POST", "/api/v1/alertas/1/reconocimiento"), ("POST", "/api/v1/maniobras"),
            ("DELETE", "/api/v1/maniobras/1"), ("GET", "/api/v1/config/umbrales"),
            ("PUT", "/api/v1/config/umbrales"), ("POST", "/api/v1/config/umbrales/1/aprobacion"),
            ("GET", "/api/v1/version")]
seg = carpeta("RNF · Seguridad", [
    carpeta("CP-34 · Acceso sin autenticar", [
        req("%s %s → 401" % (m, r), m, r, "{}" if m in ("POST", "PUT") else None, None, [codigo(401)])
        for m, r in sin_cred
    ]),
])

coleccion = {
    "info": {"_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "atalaya-3.1.0")),
             "name": "ATALAYA 3.1.0 · Grupo E",
             "description": "Casos de la planilla marcados con Postman. Requiere el SISTEMA con el perfil qa "
                            "(./mvnw spring-boot:run -Dspring-boot.run.profiles=qa) y el entorno qa. Cada caso "
                            "arranca con /qa/reset (precondición Base). Correr cada carpeta en orden.",
             "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
    "item": [rf01, rf03, rf06, rf07, rf09, rf0308, rf12, seg],
}
usuarios = ["adquisicion", "operador", "jefe", "supervisor", "auditor"]
entorno = {"id": str(uuid.uuid5(uuid.NAMESPACE_URL, "atalaya-qa")), "name": "ATALAYA qa",
           "values": [{"key": "baseUrl", "value": "http://localhost:8080", "enabled": True}]
           + [v for u in usuarios for v in (
               {"key": "usr_" + u, "value": u, "enabled": True},
               {"key": "pwd_" + u, "value": u + "123", "type": "secret", "enabled": True})]
           + [{"key": "alertaId", "value": "", "enabled": True},
              {"key": "inyectadas", "value": "0", "enabled": True}],
           "_postman_variable_scope": "environment"}
with open(destino + "/ATALAYA.postman_collection.json", "w", encoding="utf-8") as f:
    json.dump(coleccion, f, ensure_ascii=False, indent=2)
with open(destino + "/qa.postman_environment.json", "w", encoding="utf-8") as f:
    json.dump(entorno, f, ensure_ascii=False, indent=2)
print("ok")
