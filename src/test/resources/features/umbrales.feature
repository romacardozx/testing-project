# language: es
Característica: Cambio de umbrales con doble aprobación
  Para que nadie cambie solo los umbrales de riesgo
  como responsable de seguridad
  quiero que un umbral solo cambie con la aprobación de un segundo rol

  Antecedentes:
    Dado que el sistema está en la precondición base
    Y el supervisor de SO solicitó cambiar el umbral alto a 35

  # CP-33 · Tabla de decisión: sin aprobar
  @CP-33
  Escenario: Sin aprobación el umbral no cambia
    Entonces el umbral alto vigente es 40

  # CP-33 · Tabla de decisión: aprueba el mismo usuario
  @CP-33
  Escenario: El supervisor que solicitó no puede aprobar
    Cuando el usuario "supervisor" intenta aprobar la solicitud
    Entonces la operación se rechaza con HTTP 403
    Y el umbral alto vigente es 40

  # CP-33 · Tabla de decisión: aprueba el jefe de turno
  @CP-33
  Escenario: Con la aprobación del jefe de turno el umbral cambia
    Cuando el usuario "jefe" intenta aprobar la solicitud
    Entonces la operación se acepta
    Y el umbral alto vigente es 35
