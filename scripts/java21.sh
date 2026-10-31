#!/usr/bin/env bash
# Imprime el JAVA_HOME de un JDK 21 instalado (o nada si no hay ninguno). Lo usa el Makefile para
# correr todo con Java 21 aunque el `java` por defecto sea otra versión (por ejemplo, 26).
shopt -s nullglob

es21() { [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | head -1 | grep -qE 'version "21[."]'; }

candidatos=()
[ -n "${JAVA_HOME:-}" ] && candidatos+=("$JAVA_HOME")
if command -v java >/dev/null; then
  candidatos+=("$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")")
fi
if [ -x /usr/libexec/java_home ]; then
  candidatos+=("$(/usr/libexec/java_home -v 21 2>/dev/null)")
fi
candidatos+=(
  /usr/lib/jvm/java-21-openjdk* /usr/lib/jvm/*-21* /usr/lib/jvm/*21*
  /home/linuxbrew/.linuxbrew/opt/openjdk@21 /opt/homebrew/opt/openjdk@21 /usr/local/opt/openjdk@21
  "$HOME"/.sdkman/candidates/java/21* "$HOME"/.jdks/*21* "$HOME"/.asdf/installs/java/*21*
  /Library/Java/JavaVirtualMachines/*21*/Contents/Home
)

for c in "${candidatos[@]}"; do
  [ -n "$c" ] && es21 "$c" && { echo "$c"; exit 0; }
done
exit 0
