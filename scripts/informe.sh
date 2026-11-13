#!/usr/bin/env bash
# Genera docs/TP_ATALAYA_QA_v7.pdf a partir de docs/fuentes/TP_ATALAYA_QA_v7.html con Chrome sin ventana.
# Usa el Chrome que baja Selenium Manager (make install-chrome) o uno instalado en el sistema.
set -euo pipefail
cd "$(dirname "$0")/.."
FUENTE=docs/fuentes/TP_ATALAYA_QA_v7.html
SALIDA=docs/TP_ATALAYA_QA_v7.pdf
CHROME=$(ls -d "$HOME"/.cache/selenium/chrome/*/*/chrome 2>/dev/null | sort -V | tail -1 || true)
[ -n "$CHROME" ] || CHROME=$(command -v google-chrome || command -v chromium || command -v chromium-browser || true)
[ -n "$CHROME" ] || { echo "  ✘ no encontré Chrome: correr make install-chrome"; exit 1; }
"$CHROME" --headless=new --disable-gpu --no-pdf-header-footer --run-all-compositor-stages-before-draw \
  --virtual-time-budget=10000 --print-to-pdf="$SALIDA" "file://$PWD/$FUENTE" 2>/dev/null
echo "  ✔ $SALIDA · $(pdfinfo "$SALIDA" 2>/dev/null | awk '/^Pages/ {print $2}') páginas"
