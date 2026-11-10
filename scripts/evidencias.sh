#!/usr/bin/env bash
# Corre cada tipo de prueba y guarda la salida como evidencia de ejecución en docs/evidencias/.
#   ejecucion/suite-completa.txt   mvn verify con el árbol ✔/✘ de cada caso (CP-xx, UI-xx, INF-xx)
#   ejecucion/cobertura.txt        resumen de JaCoCo por paquete
#   ejecucion/pit.txt              resumen de PIT (score y mutantes vivos)
#   postman/newman.txt, .xml       la colección de Postman corrida con newman
#   selenium/*.png, ui-chrome.txt  UI-01 a UI-03 en Chrome sin ventana, una captura por prueba
# Necesita Chrome (make instalar-chrome) y newman (make instalar).
set -euo pipefail
cd "$(dirname "$0")/.."
export NO_COLOR=1
E=docs/evidencias
mkdir -p $E/ejecucion $E/postman $E/selenium
fecha() { date '+%d/%m/%Y %H:%M'; }
cabecera() { printf 'ATALAYA 3.1.0 · %s · %s · %s\n\n' "$1" "$(fecha)" "$(java -version 2>&1 | head -1)"; }

echo "1/4 · suite completa";  { cabecera "make todo"; make -s todo; } > $E/ejecucion/suite-completa.txt 2>&1
scripts/cobertura-resumen.sh > $E/ejecucion/cobertura.txt
echo "2/4 · PIT";             { cabecera "make pit"; make -s pit; } > $E/ejecucion/pit.txt 2>&1
echo "3/4 · Postman";         { cabecera "make postman"; make -s postman; } > $E/postman/newman.txt 2>&1
cp target/newman/postman.xml $E/postman/newman.xml
echo "4/4 · Selenium";        rm -f $E/selenium/*.png
{ cabecera "make ui-chrome HEADLESS=true"; ./mvnw -B -q test -Dconsola=true -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtest='**/ui/*Test' -Dbrowser=chrome -Dheadless=true -Dcapturas=$E/selenium; } > $E/selenium/ui-chrome.txt 2>&1
ls $E/ejecucion $E/postman $E/selenium
