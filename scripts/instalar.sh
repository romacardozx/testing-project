#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."

if [ -z "${NO_COLOR:-}" ]; then V=$'\e[32m'; R=$'\e[31m'; A=$'\e[33m'; N=$'\e[1m'; D=$'\e[2m'; X=$'\e[0m'
else V=; R=; A=; N=; D=; X=; fi
ok()    { printf "  ${V}✔${X} %s\n" "$1"; }
falla() { printf "  ${R}✘ %s${X}\n" "$1"; exit 1; }
aviso() { printf "  ${A}!${X} %s\n" "$1"; }
paso()  { printf "\n${N}%s${X}\n" "$1"; }
mvn()   { ./mvnw -B -q "$@" > target/instalar.log 2>&1 || { tail -20 target/instalar.log; falla "falló: mvnw $*"; }; }
mkdir -p target

paso "1/4 · Java 21"
jdk21=$(scripts/java21.sh)
if [ -z "$jdk21" ]; then
  actual=$(java -version 2>&1 | head -1)
  falla "No encontré un JDK 21 (el java por defecto es: ${actual:-ninguno}). Instalarlo, por ejemplo:
      Ubuntu/Debian: sudo apt install openjdk-21-jdk
      macOS:         brew install openjdk@21
      SDKMAN:        sdk install java 21-tem
    o apuntar JAVA_HOME a un JDK 21 y repetir make install."
fi
export JAVA_HOME="$jdk21" PATH="$jdk21/bin:$PATH"
ok "$(java -version 2>&1 | head -1) · $jdk21"
por_defecto="${JAVA_POR_DEFECTO:-$(java -version 2>&1 | head -1)}"
echo "$por_defecto" | grep -qE 'version "21[."]' || \
  printf "  ${D}↷ el java por defecto es otro (%s): el Makefile usa el JDK 21 igual${X}\n" "$por_defecto"

paso "2/4 · Dependencias de Maven (la primera vez tarda unos minutos)"
mvn dependency:go-offline
ok "dependencias y plugins del pom"
mvn test -Dtest=RelojMutableTest -Dsurefire.failIfNoSpecifiedTests=false
ok "compilación y proveedor JUnit Platform de Surefire"
mvn test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=ar.unsam.vv.atalaya.dominio.NivelRiesgo \
    -DtargetTests=ar.unsam.vv.atalaya.dominio.AlertasTest -DreportsDirectory=target/pit-calentamiento
ok "PIT y pitest-junit5-plugin"

paso "3/4 · newman (Postman por línea de comandos)"
if command -v npm >/dev/null; then
  npm ci --no-audit --no-fund --loglevel=error > target/instalar.log 2>&1 || { tail -20 target/instalar.log; falla "falló npm ci"; }
  ok "newman $(node_modules/.bin/newman --version) en node_modules/ (versión fija en package-lock.json)"
else
  aviso "No hay Node.js/npm: make test-postman no va a andar. Instalar Node 18 o más nuevo y repetir make install."
fi

paso "4/4 · Chrome para Selenium"
if [ "${1:-}" = "--chrome" ]; then
  mvn test -Dtest=PanelUiTest -Dbrowser=chrome -Dheadless=true
  ok "Chrome for Testing y chromedriver en ~/.cache/selenium (Selenium Manager)"
else
  printf "  ${D}↷ se baja solo la primera vez que se corre make test-ui-chrome / test-ui-demo (o: make install-chrome)${X}\n"
fi

printf "\n${V}${N}Listo.${X} Probá: ${N}make test${X}\n\n"
