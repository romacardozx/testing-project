# language: es
Característica: Agrupación de alertas durante una maniobra
  Para no perder atención con alertas esperables
  como operador de reactor
  quiero que durante una maniobra las variaciones dentro de la banda generen una sola alerta informativa

  Antecedentes:
    Dado que el sistema está en la precondición base

  # CP-27 · Partición de equivalencia: dentro de la banda
  @CP-27
  Escenario: Las variaciones esperables generan una sola alerta informativa
    Dado que la potencia térmica está en 80 en los tres canales
    Y el jefe de turno declara una maniobra con banda de potencia de 70 a 90
    Cuando llegan 10 estimaciones de nivel ALTO
    Entonces hay exactamente 1 alerta
    Y es una alerta INFORMATIVA que agrupa 10 estimaciones

  # CP-28 · Valor límite: apenas fuera de la banda
  @CP-28
  Escenario: Fuera de la banda se alerta normalmente
    Dado que la potencia térmica está en 90,1 en los tres canales
    Y el jefe de turno declara una maniobra con banda de potencia de 70 a 90
    Cuando llegan 1 estimaciones de nivel ALTO
    Entonces hay exactamente 1 alerta
    Y es una alerta de RIESGO
