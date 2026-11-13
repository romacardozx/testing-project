#!/usr/bin/env bash
# Arma el reporte HTML de la última corrida en target/reporte-pruebas/index.html y, con --abrir, lo abre.
#   pruebas.js     lo escribe ResumenConsola al terminar cualquier make test*
#   cobertura.js   desde target/site/jacoco/jacoco.csv (si existe: la deja make test)
#   pit.js         desde target/pit-reports/mutations.xml (si existe: la deja make mutation)
# La página es estática (scripts/reporte/index.html) y no usa nada de internet.
set -euo pipefail
cd "$(dirname "$0")/.."
DIR=target/reporte-pruebas
CSV=target/site/jacoco/jacoco.csv
XML=target/pit-reports/mutations.xml

[ -f "$DIR/pruebas.js" ] || { echo "  ✘ no hay resultados en $DIR: correr make test (o cualquier make test-*)"; exit 1; }
cp scripts/reporte/index.html "$DIR/index.html"
fecha() { date -r "$1" '+%d/%m/%Y %H:%M'; }

rm -f "$DIR/cobertura.js" "$DIR/pit.js"
if [ -f "$CSV" ]; then
  awk -F, -v fecha="$(fecha "$CSV")" '
    NR > 1 {
      p = $2; sub(/^ar\.unsam\.vv\.atalaya\.?/, "", p); if (p == "") p = "(app)"
      if (!(p in lm)) orden[++n] = p
      lm[p] += $8; lc[p] += $9; bm[p] += $6; bc[p] += $7
    }
    END {
      printf "window.ATALAYA_COBERTURA = {\"fecha\": \"%s\", \"paquetes\": [", fecha
      for (i = 1; i <= n; i++) { p = orden[i]
        printf "%s{\"nombre\": \"%s\", \"lineas\": [%d, %d], \"ramas\": [%d, %d]}", (i > 1 ? ", " : ""), p, lc[p], lc[p] + lm[p], bc[p], bc[p] + bm[p] }
      print "]};"
    }' "$CSV" > "$DIR/cobertura.js"
fi

if [ -f "$XML" ]; then
  awk -v fecha="$(fecha "$XML")" '
    function campo(etiqueta,   ini, fin) {
      ini = index($0, "<" etiqueta ">"); if (!ini) return "";
      ini += length(etiqueta) + 2; fin = index(substr($0, ini), "</" etiqueta ">");
      return substr($0, ini, fin - 1)
    }
    function js(s) { gsub(/&lt;/, "<", s); gsub(/&gt;/, ">", s); gsub(/&quot;/, "\"", s); gsub(/&amp;/, "\\&", s)
                     gsub(/\\/, "\\\\", s); gsub(/"/, "\\\"", s); return "\"" s "\"" }
    /<mutation / {
      match($0, /status=.[A-Z_]+./); estado = substr($0, RSTART + 8, RLENGTH - 9)
      clase = campo("sourceFile"); if (!(clase in total)) orden[++n] = clase
      total[clase]++; if (estado == "KILLED") muertos[clase]++
      if (estado != "KILLED")
        vivos[++nv] = sprintf("{\"clase\": %s, \"linea\": %d, \"estado\": %s, \"descripcion\": %s}",
                              js(clase), campo("lineNumber"), js(estado), js(campo("description")))
    }
    END {
      printf "window.ATALAYA_PIT = {\"fecha\": \"%s\", \"clases\": [", fecha
      for (i = 1; i <= n; i++) { k = orden[i]
        printf "%s{\"nombre\": %s, \"muertos\": %d, \"total\": %d}", (i > 1 ? ", " : ""), js(k), muertos[k], total[k] }
      printf "], \"vivos\": ["
      for (i = 1; i <= nv; i++) printf "%s%s", (i > 1 ? ", " : ""), vivos[i]
      print "]};"
    }' "$XML" > "$DIR/pit.js"
fi

PAGINA="$PWD/$DIR/index.html"
if [ "${1:-}" != "--abrir" ]; then
  echo "  ✔ $DIR/index.html"
  exit 0
fi
for abrir in xdg-open open wslview; do
  if command -v "$abrir" >/dev/null; then
    "$abrir" "$PAGINA" >/dev/null 2>&1 & disown
    echo "  ✔ reporte abierto en el navegador: $DIR/index.html"
    exit 0
  fi
done
echo "  ↷ no encontré cómo abrir el navegador; abrir a mano: file://$PAGINA"
