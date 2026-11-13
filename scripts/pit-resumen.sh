#!/usr/bin/env bash
set -euo pipefail
xml="${1:-target/pit-reports/mutations.xml}"
[ -f "$xml" ] || { echo "No existe $xml. Corré: make mutation"; exit 1; }

if [ -z "${NO_COLOR:-}" ]; then V=$'\e[32m'; R=$'\e[31m'; A=$'\e[33m'; C=$'\e[36m'; N=$'\e[1m'; T=$'\e[2m'; X=$'\e[0m'
else V=; R=; A=; C=; N=; T=; X=; fi

awk -v V="$V" -v R="$R" -v A="$A" -v C="$C" -v N="$N" -v T="$T" -v X="$X" '
  function campo(etiqueta,   ini, fin) {
    ini = index($0, "<" etiqueta ">"); if (!ini) return "";
    ini += length(etiqueta) + 2; fin = index(substr($0, ini), "</" etiqueta ">");
    return substr($0, ini, fin - 1)
  }
  /<mutation / {
    match($0, /status=.[A-Z_]+./); estado = substr($0, RSTART + 8, RLENGTH - 9)
    total++; cuenta[estado]++
    clase = campo("sourceFile"); porClase[clase]++; if (estado == "KILLED") muertos[clase]++
    if (estado != "KILLED") {
      d = campo("description"); gsub(/&lt;/, "<", d); gsub(/&gt;/, ">", d); gsub(/&amp;/, "\\&", d)
      vivos[++nv] = sprintf("  %s%-12s%s %s:%s  %s%s%s", A, estado, X, clase, campo("lineNumber"), T, d, X)
    }
  }
  END {
    score = total ? 100 * cuenta["KILLED"] / total : 0
    col = score >= 75 ? V : R
    printf "\n%s%s┌─ PIT · mutation testing (dominio) ───────────────────────────┐%s\n", C, N, X
    printf "%s%s│%s  Mutantes %-4d  muertos %-4d  vivos %-3d  sin cobertura %-3d %s%s│%s\n", C, N, X, total, cuenta["KILLED"], cuenta["SURVIVED"], cuenta["NO_COVERAGE"], C, N, X
    printf "%s%s│%s  Mutation score: %s%s%5.1f %%%s  (umbral del informe: 75 %%)       %s%s│%s\n", C, N, X, col, N, score, X, C, N, X
    printf "%s%s└──────────────────────────────────────────────────────────────┘%s\n\n", C, N, X
    printf "%s%-32s %8s %8s%s\n", N, "Clase", "Muertos", "Score", X
    n = 0; for (k in porClase) orden[++n] = k
    for (i = 1; i <= n; i++) for (j = i + 1; j <= n; j++) if (orden[j] < orden[i]) { t = orden[i]; orden[i] = orden[j]; orden[j] = t }
    for (i = 1; i <= n; i++) {
      k = orden[i]; s = 100 * muertos[k] / porClase[k]
      printf "%-32s %4d/%-3d %s%7.1f%%%s\n", k, muertos[k], porClase[k], (s == 100 ? V : (s >= 75 ? A : R)), s, X
    }
    if (nv) { printf "\n%sNo muertos (analizar en docs/pit-analisis.md):%s\n", N, X; for (i = 1; i <= nv; i++) print vivos[i] }
    printf "\n%sReporte HTML: target/pit-reports/index.html%s\n\n", T, X
  }' "$xml"
