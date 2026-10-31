# ATALAYA 3.1.0 · comandos de pruebas (TP VyV · UNSAM · Grupo E)
# Requiere Java 21 y bash. Maven no hace falta: se usa ./mvnw.
# `make` sin argumentos muestra la ayuda. Sin colores: NO_COLOR=1 make <objetivo>.

# Java 21 aunque el `java` por defecto sea otra versión: se busca un JDK 21 instalado (scripts/java21.sh)
# y se exporta para ./mvnw y los scripts.
JAVA21 := $(shell scripts/java21.sh)
export JAVA_POR_DEFECTO := $(shell java -version 2>&1 | head -1)
ifneq ($(JAVA21),)
  export JAVA_HOME := $(JAVA21)
  export PATH := $(JAVA21)/bin:$(PATH)
endif

MVN      := ./mvnw -B -q
CONSOLA  := -Dconsola=true -Dsurefire.failIfNoSpecifiedTests=false
TEST     := $(MVN) test $(CONSOLA)

ifeq ($(NO_COLOR),)
  C := \033[36m
  N := \033[1m
  D := \033[2m
  X := \033[0m
endif

.DEFAULT_GOAL := ayuda
.PHONY: ayuda instalar instalar-chrome todo unitarias api integracion bdd ui ui-chrome ui-demo clase pit cobertura verify \
        app postman reportes limpiar

ayuda: ## Muestra esta ayuda
	@printf "\n$(N)ATALAYA 3.1.0 · pruebas$(X)\n\n"
	@awk 'BEGIN {FS = ":.*## "} /^[a-z-]+:.*## / {printf "  $(C)make %-12s$(X) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@printf "\n$(D)Ejemplos: make clase T=ValidadorRedundanciaTest · make ui-chrome · NO_COLOR=1 make todo$(X)\n\n"

instalar: ## Descarga todo lo necesario: dependencias Maven, plugins (Surefire, PIT) y newman
	@scripts/instalar.sh

instalar-chrome: ## Igual que instalar, y además baja Chrome y chromedriver para Selenium
	@scripts/instalar.sh --chrome

todo: ## Toda la suite (mvn verify): unitarias, API, integración, BDD, Selenium + JaCoCo
	@$(MVN) verify $(CONSOLA)
	@scripts/cobertura-resumen.sh

unitarias: ## Unitarias de dominio (caja negra y blanca, TDD; las que usa PIT)
	@$(TEST) -Dtest='**/dominio/**/*Test,**/config/*Test'

api: ## API con MockMvc y contra el servidor real (CP-01, 03, 13-15, 24, 34, 35)
	@$(TEST) -Dtest='**/api/*Test'

integracion: ## Integración con H2 y reloj de QA: caja gris, concurrencia, pairwise (CP-31)
	@$(TEST) -Dtest='**/integracion/*Test'

bdd: ## Escenarios Cucumber en español (CP-07, 21, 23, 27, 28, 33)
	@$(TEST) -Dtest=CucumberTest
	@printf "$(D)Reporte HTML: target/cucumber-reports/cucumber.html$(X)\n\n"

ui: ## Selenium sobre el panel con HtmlUnit, sin navegador (UI-01 a UI-03)
	@$(TEST) -Dtest='**/ui/*Test'

ui-chrome: ## Selenium con Chrome visible; se cierra solo (HEADLESS=true para ocultarlo)
	@$(TEST) -Dtest='**/ui/*Test' -Dbrowser=chrome -Dheadless=$(or $(HEADLESS),false)

ui-demo: ## Chrome queda abierto al final de cada prueba (franja verde/roja); cerrar la ventana sigue
	@printf "$(D)Modo pausa: al terminar cada prueba cerrá la ventana de Chrome para seguir (o esperá 10 min).$(X)\n"
	@$(TEST) -Dtest='$(or $(T),**/ui/*Test)' -Dbrowser=chrome -Dpausa=true

clase: ## Una clase o patrón: make clase T=PairwiseIntegracionTest
	@test -n "$(T)" || { printf "Falta T. Ejemplo: make clase T=ServicioEscalamientoTest\n"; exit 1; }
	@$(TEST) -Dtest='$(T)'

pit: ## Mutation testing con PIT sobre dominio y resumen por clase
	@printf "$(D)Compilando y corriendo PIT (salida completa en target/pit.log)...$(X)\n"
	@$(MVN) test-compile org.pitest:pitest-maven:mutationCoverage > target/pit.log 2>&1 \
		|| { tail -30 target/pit.log; exit 1; }
	@scripts/pit-resumen.sh

cobertura: ## Corre la suite y muestra la cobertura JaCoCo por paquete
	@$(MVN) verify $(CONSOLA)
	@scripts/cobertura-resumen.sh

verify: ## mvn verify tal cual lo corre la cátedra (salida estándar de Maven)
	@./mvnw -B verify

app: ## Levanta el SISTEMA con el perfil qa en http://localhost:8080 (panel en /panel)
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=qa

postman: ## Corre la colección de Postman con newman; levanta y apaga la app si no está corriendo
	@scripts/postman.sh

reportes: ## Copia los reportes de JaCoCo, PIT y Cucumber a reportes/ (para el ZIP)
	@rm -rf reportes && mkdir -p reportes
	@for d in site/jacoco:jacoco pit-reports:pit-reports cucumber-reports:cucumber; do \
		src=target/$${d%%:*}; dst=reportes/$${d##*:}; \
		if [ -d $$src ]; then cp -r $$src $$dst && printf "  ✔ $$dst\n"; \
		else printf "  ↷ falta $$src (correr make todo / make pit)\n"; fi; done

limpiar: ## Borra target/ y reportes/
	@./mvnw -B -q clean && rm -rf reportes
	@printf "  ✔ limpio\n"
