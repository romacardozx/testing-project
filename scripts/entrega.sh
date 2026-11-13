#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

NOMBRE=VV-Q2-2026-TN-GrupoE-ATALAYA
ZIP="$PWD/$NOMBRE.zip"
DESTINO=target/entrega/$NOMBRE
LISTA=.distignore

if [ -z "${NO_COLOR:-}" ]; then V=$'\e[32m'; R=$'\e[31m'; A=$'\e[33m'; N=$'\e[1m'; X=$'\e[0m'; else V= R= A= N= X=; fi
paso() { printf "\n%s%s%s\n" "$N" "$1" "$X"; }
ok()   { printf "  %s✔%s %s\n" "$V" "$X" "$1"; }
mal()  { printf "  %s✘ %s%s\n" "$R" "$1" "$X"; exit 1; }

PROHIBIDO='^target/|\.zip$'
LINEAS_README=()
if [ -f "$LISTA" ]; then
  PROHIBIDO=$(grep -v -e '^readme:' -e '^$' "$LISTA" | paste -sd'|')
  mapfile -t LINEAS_README < <(sed -n 's/^readme://p' "$LISTA")
fi
EXCLUIR="$PROHIBIDO|^reportes/"

paso "1/4 · Pruebas"
if [ "${1:-}" = "--sin-pruebas" ]; then
  printf "  %s↷ se reusan los resultados de target/ (sin correr la suite)%s\n" "$A" "$X"
else
  log=$(mktemp)
  ./mvnw -B -q clean verify > "$log" 2>&1 \
    && ok "mvn clean verify en verde" \
    || { tail -30 "$log"; mal "mvn verify falló: no se arma el ZIP (log completo: $log)"; }
  rm -f "$log"
  ./mvnw -B -q test-compile org.pitest:pitest-maven:mutationCoverage > target/pit.log 2>&1 \
    && ok "PIT ($(grep -o 'Generated [0-9]* mutations Killed [0-9]* ([0-9]*%)' target/pit.log | tail -1))" \
    || mal "PIT falló (log: target/pit.log)"
fi

paso "2/4 · Reportes"
rm -rf reportes && mkdir -p reportes
for d in site/jacoco:jacoco pit-reports:pit-reports cucumber-reports:cucumber; do
  src=target/${d%%:*}; dst=reportes/${d##*:}
  [ -d "$src" ] || mal "falta $src (correr sin SKIP_TESTS)"
  cp -r "$src" "$dst" && ok "$dst"
done

paso "3/4 · Copia de archivos"
rm -rf target/entrega && mkdir -p "$DESTINO"
excluidos=0
while IFS= read -r archivo; do
  [ -f "$archivo" ] || continue
  if [[ "$archivo" =~ $EXCLUIR ]]; then excluidos=$((excluidos + 1)); continue; fi
  mkdir -p "$DESTINO/$(dirname "$archivo")"
  cp -p "$archivo" "$DESTINO/$archivo"
done < <(git ls-files -co --exclude-standard)
cp -r reportes "$DESTINO/reportes"
mkdir -p "$DESTINO/docs/evidencias/postman" "$DESTINO/docs/evidencias/selenium"
for texto in "${LINEAS_README[@]}"; do
  grep -vF "$texto" "$DESTINO/README.md" > "$DESTINO/README.tmp" || true
  mv "$DESTINO/README.tmp" "$DESTINO/README.md"
done
ok "$(find "$DESTINO" -type f | wc -l) archivos copiados · $excluidos excluidos"

paso "4/4 · ZIP"
rm -f "$ZIP"
(cd target/entrega && zip -q -r -X "$ZIP" "$NOMBRE")
sobrantes=$(unzip -Z1 "$ZIP" | sed "s|^$NOMBRE/||" | grep -E "$PROHIBIDO" || true)
[ -z "$sobrantes" ] || mal "el ZIP tiene archivos que no van: $sobrantes"
for texto in "${LINEAS_README[@]}"; do
  ! unzip -p "$ZIP" "$NOMBRE/README.md" | grep -qF "$texto" || mal "el README del ZIP todavía menciona $texto"
done
ok "sin archivos de trabajo internos"
ok "$NOMBRE.zip · $(du -h "$ZIP" | cut -f1)"
if grep -q '\[rol\]' docs/fuentes/TP_ATALAYA_QA_v7.html 2>/dev/null; then
  printf "\n  %s↷ el informe todavía tiene [rol] en la sección 6: completarlo, correr make docs y make dist%s\n" "$A" "$X"
fi
printf "\n"
