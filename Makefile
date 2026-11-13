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

MVN     := ./mvnw -B -q
# Resumen de consola (ResumenConsola). Recursiva (=) para que $@ sea el objetivo que la usa.
CONSOLA  = -Dconsola=true -Dobjetivo=$@ -Dsurefire.failIfNoSpecifiedTests=false

# Corre Maven con el resumen de consola. Con OPEN=1 arma y abre el reporte HTML aunque haya fallos;
# el código de salida es siempre el de Maven.
REPORTE  = $(if $(OPEN),scripts/reporte.sh --abrir,true)
correr   = $(MVN) $(1) $(CONSOLA) $(2); st=$$?; $(3) $(REPORTE); exit $$st

UNITARIAS := **/dominio/**/*Test,**/config/*Test,**/aplicacion/*Test
UI        := **/ui/*Test

ifeq ($(NO_COLOR),)
  C := \033[36m
  N := \033[1m
  D := \033[2m
  X := \033[0m
endif

.DEFAULT_GOAL := help
.PHONY: help install install-chrome test test-unit test-api test-integration test-bdd test-ui test-ui-chrome \
        test-ui-demo test-postman mutation verify report run evidence docs dist clean distclean

##@ Pruebas  (OPEN=1 abre el reporte HTML al terminar · T=<Clase> corre una clase o patrón)

test: ## Toda la suite (mvn verify) + cobertura JaCoCo · con T=<Clase>, solo esa clase
	@$(if $(T),$(call correr,test,-Dtest='$(T)'),$(call correr,verify,,[ $$st -ne 0 ] || scripts/cobertura-resumen.sh;))

test-unit: ## Unitarias: dominio (caja negra y blanca, TDD; las que usa PIT), servicios con Mockito
	@$(call correr,test,-Dtest='$(UNITARIAS)')

test-api: ## API con MockMvc y contra el servidor real (CP-01, 03, 13-15, 24, 34, 35)
	@$(call correr,test,-Dtest='**/api/*Test')

test-integration: ## Integración con H2 y reloj de QA: caja gris, concurrencia, pairwise (CP-31)
	@$(call correr,test,-Dtest='**/integracion/*Test')

test-bdd: ## Escenarios Cucumber en español (CP-07, 08, 21, 23, 27, 28, 33)
	@$(call correr,test,-Dtest=CucumberTest,printf "$(D)Reporte Cucumber: target/cucumber-reports/cucumber.html$(X)\n\n";)

test-ui: ## Selenium sobre el panel con HtmlUnit, sin navegador (UI-01 a UI-03)
	@$(call correr,test,-Dtest='$(UI)')

test-ui-chrome: ## Selenium con Chrome visible; se cierra solo (HEADLESS=true para ocultarlo)
	@$(call correr,test,-Dtest='$(UI)' -Dbrowser=chrome -Dheadless=$(or $(HEADLESS),false))

test-ui-demo: ## Chrome queda abierto al final de cada prueba (franja verde/roja); cerrar la ventana sigue
	@printf "$(D)Modo pausa: al terminar cada prueba cerrá la ventana de Chrome para seguir (o esperá 10 min).$(X)\n"
	@$(call correr,test,-Dtest='$(or $(T),$(UI))' -Dbrowser=chrome -Dpausa=true)

test-postman: ## Colección de Postman con newman; levanta y apaga la app si no está corriendo
	@scripts/postman.sh

mutation: ## Mutation testing con PIT sobre dominio y resumen por clase
	@mkdir -p target && printf "$(D)Compilando y corriendo PIT (salida completa en target/pit.log)...$(X)\n"
	@$(MVN) test-compile org.pitest:pitest-maven:mutationCoverage > target/pit.log 2>&1 \
		|| { tail -30 target/pit.log; exit 1; }
	@scripts/pit-resumen.sh
	@$(REPORTE)

verify: ## mvn verify tal cual lo corre la cátedra (salida estándar de Maven)
	@./mvnw -B verify

report: ## Abre el reporte HTML de la última corrida (pruebas, cobertura y PIT si están)
	@scripts/reporte.sh --abrir

##@ SISTEMA y entrega

run: ## Levanta el SISTEMA con el perfil qa en http://localhost:8080 (panel en /panel)
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=qa

evidence: ## Corre suite, PIT, Postman y Selenium en Chrome y guarda la salida en docs/evidencias/
	@scripts/evidencias.sh

docs: ## Genera docs/TP_ATALAYA_QA_v7.pdf desde docs/fuentes/TP_ATALAYA_QA_v7.html (Chrome)
	@scripts/informe.sh

dist: ## Corre todo y arma VV-Q2-2026-TN-GrupoE-ATALAYA.zip (SKIP_TESTS=1 reusa target/)
	@scripts/entrega.sh $(if $(SKIP_TESTS),--sin-pruebas)

##@ Entorno

install: ## Descarga todo lo necesario: dependencias Maven, plugins (Surefire, PIT) y newman
	@scripts/instalar.sh

install-chrome: ## Igual que install, y además baja Chrome y chromedriver para Selenium
	@scripts/instalar.sh --chrome

clean: ## Borra lo generado: target/, reportes/ y el ZIP de la entrega
	@rm -rf target reportes VV-Q2-2026-TN-GrupoE-ATALAYA.zip
	@printf "  ✔ sin target/, reportes/ ni el ZIP\n"

distclean: clean ## clean y además node_modules/ (newman); después hace falta make install
	@rm -rf node_modules
	@printf "  ✔ sin node_modules/ $(D)(Chrome de Selenium sigue en ~/.cache/selenium)$(X)\n"

help: ## Muestra esta ayuda
	@printf "\n$(N)ATALAYA 3.1.0 · pruebas$(X)\n"
	@awk 'BEGIN {FS = ":.*## "} \
		/^##@/ {printf "\n$(N)%s$(X)\n", substr($$0, 5)} \
		/^[a-z-]+:.*## / {printf "  $(C)make %-17s$(X) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@printf "\n$(D)Ejemplos: make test T=ValidadorRedundanciaTest · make test-unit OPEN=1 · make test-ui-chrome · NO_COLOR=1 make test$(X)\n\n"
