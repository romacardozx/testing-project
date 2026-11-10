#!/usr/bin/env bash
# Arma VV-Q2-2026-TN-GrupoE-ATALAYA.zip para la entrega (sección 14 del informe).
#
# 1. Corre la suite completa (mvn clean verify) y PIT: si algo falla, no arma el ZIP.
# 2. Copia los reportes de JaCoCo, PIT y Cucumber a reportes/.
# 3. Copia al ZIP los archivos del repo (versionados y nuevos no ignorados), sin lo que no va en la
#    entrega: archivos de trabajo con el asistente (CLAUDE.md, .claude/, docs/uso-ia.md), el
#    generador de la colección (Python), la fuente HTML del informe y el informe v6 (va el v7),
#    la configuración del IDE y los .gitkeep.
# 4. Comprueba que el ZIP no tenga nada de eso.
#
# El repo no se modifica: solo se excluyen archivos del ZIP. El uso de IA (condición 11.f) se
# declara en la sección 13 del informe.
#
# Uso: scripts/entrega.sh [--sin-pruebas]   (--sin-pruebas reusa target/ sin volver a correr la suite)
set -euo pipefail
cd "$(dirname "$0")/.."

NOMBRE=VV-Q2-2026-TN-GrupoE-ATALAYA
ZIP="$PWD/$NOMBRE.zip"
DESTINO=target/entrega/$NOMBRE

if [ -z "${NO_COLOR:-}" ]; then V=$'\e[32m'; R=$'\e[31m'; A=$'\e[33m'; N=$'\e[1m'; X=$'\e[0m'; else V= R= A= N= X=; fi
paso() { printf "\n%s%s%s\n" "$N" "$1" "$X"; }
ok()   { printf "  %s✔%s %s\n" "$V" "$X" "$1"; }
mal()  { printf "  %s✘ %s%s\n" "$R" "$1" "$X"; exit 1; }

# Lo que nunca va en el ZIP (expresiones regulares sobre la ruta relativa).
PROHIBIDO='^(CLAUDE\.md|\.claude/|docs/uso-ia\.md|docs/fuentes/|docs/TP_ATALAYA_QA_v6\.pdf|postman/generar_coleccion\.py|\.idea/|\.gitignore$|node_modules/|target/)|(^|/)\.gitkeep$|\.zip$'
# Al copiar se saltea además reportes/ del repo: se copia aparte, recién generado.
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
  [ -d "$src" ] || mal "falta $src (correr sin --sin-pruebas)"
  cp -r "$src" "$dst" && ok "$dst"
done

paso "3/4 · Copia de archivos"
rm -rf target/entrega && mkdir -p "$DESTINO"
excluidos=0
while IFS= read -r archivo; do
  [ -f "$archivo" ] || continue                       # borrados todavía versionados
  if [[ "$archivo" =~ $EXCLUIR ]]; then excluidos=$((excluidos + 1)); continue; fi
  mkdir -p "$DESTINO/$(dirname "$archivo")"
  cp -p "$archivo" "$DESTINO/$archivo"
done < <(git ls-files -co --exclude-standard)
cp -r reportes "$DESTINO/reportes"
mkdir -p "$DESTINO/docs/evidencias/postman" "$DESTINO/docs/evidencias/selenium"

# El README del ZIP no menciona los archivos excluidos.
sed -i -e '/generar_coleccion\.py/,+1d' -e '/uso-ia\.md/d' -e '/CLAUDE\.md/d' "$DESTINO/README.md"
ok "$(find "$DESTINO" -type f | wc -l) archivos copiados · $excluidos excluidos"

paso "4/4 · ZIP"
rm -f "$ZIP"
(cd target/entrega && zip -q -r -X "$ZIP" "$NOMBRE")
sobrantes=$(unzip -Z1 "$ZIP" | sed "s|^$NOMBRE/||" | grep -E "$PROHIBIDO" || true)
[ -z "$sobrantes" ] || mal "el ZIP tiene archivos que no van: $sobrantes"
if unzip -p "$ZIP" "$NOMBRE/README.md" | grep -q -E 'CLAUDE\.md|uso-ia|generar_coleccion'; then
  mal "el README del ZIP todavía menciona archivos excluidos"
fi
ok "sin CLAUDE.md, .claude/, docs/uso-ia.md, informe v6 ni fuentes, generador Python, .idea/ ni .gitkeep"
ok "$NOMBRE.zip · $(du -h "$ZIP" | cut -f1)"
if grep -q '\[rol\]' docs/fuentes/TP_ATALAYA_QA_v7.html 2>/dev/null; then
  printf "\n  %s↷ el informe todavía tiene [rol] en la sección 6: completarlo, correr make informe y make entrega%s\n" "$A" "$X"
fi
printf "\n"
