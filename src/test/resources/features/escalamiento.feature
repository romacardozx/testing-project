# language: es
Característica: Escalamiento de alertas críticas
  Para que una alerta crítica no quede sin atender
  como supervisor de seguridad operacional
  quiero recibirla si nadie la reconoce en 45 segundos

  Antecedentes:
    Dado que el sistema está en la precondición base

  # CP-21 · Valores límite de tiempo
  Esquema del escenario: A los <segundos> s sin reconocer la alerta queda <estado>
    Dado una alerta crítica emitida al operador de reactor
    Cuando pasan <segundos> segundos sin reconocimiento
    Entonces el estado de la alerta es <estado>
    Y la alerta tiene <destinatarios> destinatarios

    Ejemplos:
      | segundos | estado   | destinatarios |
      | 44       | EMITIDA  | 2             |
      | 45       | ESCALADA | 3             |
      | 46       | ESCALADA | 3             |

  # CP-23 · Transición de estados: reconocer antes del plazo
  Escenario: Reconocer antes del plazo evita el escalamiento
    Dado una alerta crítica emitida al operador de reactor
    Y pasan 44 segundos sin reconocimiento
    Cuando el usuario "operador" reconoce la alerta
    Y se llega a los 60 segundos desde la emisión
    Entonces el estado de la alerta es RECONOCIDA
    Y la alerta tiene 2 destinatarios
