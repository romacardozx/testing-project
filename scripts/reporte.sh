#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DIR=target/reporte-pruebas
CSV=target/site/jacoco/jacoco.csv
XML=target/pit-reports/mutations.xml

[ -f "$DIR/pruebas.js" ] || { echo "  ✘ no hay resultados en $DIR: correr make test (o cualquier make test-*)"; exit 1; }
cp scripts/reporte/index.html "$DIR/index.html"
fecha() { date -r "$1" '+%d/%m/%Y %H:%M'; }

rm -f "$DIR/cobertura.js" "$DIR/pit.js"

tsv_a_js() {
  awk -F'\t' -v var="$1" '
    function js(s) { gsub(/\\/, "\\\\", s); gsub(/"/, "\\\"", s); return "\"" s "\"" }
    NR == 1 { for (i = 1; i <= NF; i++) col[i] = $i; printf "window.%s = [", var; next }
    { printf "%s{", (NR > 2 ? ", " : ""); for (i = 1; i <= NF; i++) printf "%s%s: %s", (i > 1 ? ", " : ""), js(col[i]), js($i); printf "}" }
    END { print "];" }' "$2" > "$3"
}
tsv_a_js ATALAYA_CATALOGO scripts/reporte/catalogo.tsv "$DIR/catalogo.js"
tsv_a_js ATALAYA_DEFECTOS scripts/reporte/defectos.tsv "$DIR/defectos.js"

find src/test/java -name '*Test.java' | sort | xargs awk '
  function js(s) { gsub(/\\/, "\\\\", s); gsub(/"/, "\\\"", s); return "\"" s "\"" }
  FNR == 1 { doc = ""; dentro = 0; listo = 0; clase = FILENAME; sub(/.*\//, "", clase); sub(/\.java$/, "", clase) }
  listo { next }
  /^\/\*\*/ { dentro = 1; doc = ""; sub(/^\/\*\*/, "") }
  dentro {
    fin = sub(/\*\/.*/, ""); linea = $0; sub(/^[ \t]*\*?[ \t]?/, "", linea); doc = doc " " linea
    if (fin) dentro = 0
    next
  }
  /^(public |abstract |final )*class / { listo = 1
    gsub(/\{@(code|link) /, "", doc); gsub(/}/, "", doc); gsub(/<\/?[a-z]+>/, " ", doc); gsub(/@[a-z]+ .*/, "", doc)
    gsub(/[ \t]+/, " ", doc); sub(/^ /, "", doc); sub(/ $/, "", doc)
    if (doc != "") salida[++n] = js(clase) ": " js(doc)
  }
  END { printf "window.ATALAYA_CLASES = {"; for (i = 1; i <= n; i++) printf "%s%s", (i > 1 ? ", " : ""), salida[i]; print "};" }' > "$DIR/clases.js"
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
