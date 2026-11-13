#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."

URL="http://localhost:8080"
NEWMAN="node_modules/.bin/newman"
LOG="target/newman/app.log"
mkdir -p target/newman

if [ ! -x "$NEWMAN" ]; then
  echo "Falta newman. Corré: make install"
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

codigo=$(curl -s -o /dev/null -w '%{http_code}' "$URL/actuator/health" || true)
if [ "$codigo" = "200" ]; then
  echo "Uso el SISTEMA que ya está levantado en $URL (tiene que tener el perfil qa)."
elif [ "$codigo" != "000" ]; then
  echo "El puerto 8080 está ocupado por otro proceso que no es ATALAYA (/actuator/health respondió $codigo)."
  echo "Liberarlo y repetir. Proceso: $(command -v ss >/dev/null && ss -ltnpH 'sport = :8080' | grep -o 'users:.*' || echo '?')"
  exit 1
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
