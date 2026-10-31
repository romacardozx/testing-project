#!/usr/bin/env bash
# Corre la colección de Postman con newman (instalado local con `make instalar`).
# Si el SISTEMA no está levantado en el puerto 8080, lo levanta con el perfil qa, espera a que
# responda, corre la colección y lo apaga al terminar (solo el proceso que lanzó este script).
# Deja el resultado en JUnit XML en target/newman/postman.xml (evidencia).
set -uo pipefail
cd "$(dirname "$0")/.."

URL="http://localhost:8080"
NEWMAN="node_modules/.bin/newman"
LOG="target/newman/app.log"
mkdir -p target/newman

if [ ! -x "$NEWMAN" ]; then
  echo "Falta newman. Corré: make instalar"
  exit 1
fi

pid_app=""
apagar() {
  if [ -n "$pid_app" ]; then
    echo "Apagando el SISTEMA (pid $pid_app)..."
    pkill -TERM -P "$pid_app" 2>/dev/null
    kill -TERM "$pid_app" 2>/dev/null
    wait "$pid_app" 2>/dev/null
  fi
}
trap apagar EXIT

if curl -sf -o /dev/null "$URL/actuator/health"; then
  echo "Uso el SISTEMA que ya está levantado en $URL (tiene que tener el perfil qa)."
else
  echo "Levantando el SISTEMA con el perfil qa (log en $LOG)..."
  ./mvnw -B -q spring-boot:run -Dspring-boot.run.profiles=qa > "$LOG" 2>&1 &
  pid_app=$!
  if ! curl -sf -o /dev/null --retry 120 --retry-delay 1 --retry-connrefused "$URL/actuator/health"; then
    echo "El SISTEMA no respondió. Últimas líneas del log:"
    tail -20 "$LOG"
    exit 1
  fi
fi

"$NEWMAN" run postman/ATALAYA.postman_collection.json \
  -e postman/qa.postman_environment.json \
  --reporters cli,junit \
  --reporter-junit-export target/newman/postman.xml \
  --color "$([ -n "${NO_COLOR:-}" ] && echo off || echo on)"
resultado=$?
echo "Resultado JUnit: target/newman/postman.xml"
exit $resultado
