#!/usr/bin/env bash
# Resumen de JaCoCo en consola a partir de target/site/jacoco/jacoco.csv.
# Umbrales del informe (sección 11) para dominio: sentencias >= 80 %, ramas >= 70 %.
set -euo pipefail
csv="${1:-target/site/jacoco/jacoco.csv}"
[ -f "$csv" ] || { echo "No existe $csv. Corré: make cobertura"; exit 1; }

if [ -z "${NO_COLOR:-}" ]; then V=$'\e[32m'; R=$'\e[31m'; C=$'\e[36m'; N=$'\e[1m'; T=$'\e[2m'; X=$'\e[0m'
else V=; R=; C=; N=; T=; X=; fi

awk -F, -v V="$V" -v R="$R" -v C="$C" -v N="$N" -v T="$T" -v X="$X" '
  NR > 1 {
    p = $2; sub(/^ar\.unsam\.vv\.atalaya\.?/, "", p); if (p == "") p = "(app)"
    lm[p] += $8; lc[p] += $9; bm[p] += $6; bc[p] += $7
    tlm += $8; tlc += $9; tbm += $6; tbc += $7
  }
  function pct(c, m) { return (c + m) ? 100 * c / (c + m) : -1 }
  function fmt(v, umbral,   col) {
    if (v < 0) return sprintf("%9s", "—")
    col = (umbral == "" ? "" : (v >= umbral ? V : R))
    return sprintf("%s%8.1f%%%s", col, v, (col == "" ? "" : X))
  }
  END {
    printf "\n%s%s┌─ JaCoCo · cobertura por paquete ─────────────────────────────┐%s\n", C, N, X
    printf "%s%s└──────────────────────────────────────────────────────────────┘%s\n", C, N, X
    printf "%s%-16s %10s %9s%s\n", N, "Paquete", "Líneas", "Ramas", X  # +1: la í ocupa 2 bytes
    n = 0; for (k in lm) orden[++n] = k
    for (i = 1; i <= n; i++) for (j = i + 1; j <= n; j++) if (orden[j] < orden[i]) { t = orden[i]; orden[i] = orden[j]; orden[j] = t }
    for (i = 1; i <= n; i++) {
      k = orden[i]
      if (k == "dominio") printf "%s%-16s%s %s %s  %s← umbral 80 / 70%s\n", N, k, X, fmt(pct(lc[k], lm[k]), 80), fmt(pct(bc[k], bm[k]), 70), T, X
      else printf "%-16s %s %s\n", k, fmt(pct(lc[k], lm[k]), ""), fmt(pct(bc[k], bm[k]), "")
    }
    printf "%s%-16s %s %s%s\n", N, "TOTAL", fmt(pct(tlc, tlm), ""), fmt(pct(tbc, tbm), ""), X
    printf "\n%sReporte HTML: target/site/jacoco/index.html%s\n\n", T, X
  }' "$csv"
