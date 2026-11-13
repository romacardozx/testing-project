# language: es
Característica: Redundancia de los canales de temperatura del primario
  Para no estimar el riesgo con un instrumento que falla
  como operador de reactor
  quiero que un canal que discrepa más del 2 % deje de usarse y se me avise

  Antecedentes:
    Dado que el sistema está en la precondición base

  # CP-07 · Tabla de decisión R4 + valores límite
  @CP-07
  Escenario: Un canal que discrepa más del 2 % se deja de usar y se avisa
    Dado que los canales A y B de TEMPERATURA_PRIMARIO informan 300,00
    Cuando el canal C de TEMPERATURA_PRIMARIO informa 306,64
    Entonces el canal C de TEMPERATURA_PRIMARIO queda NO_CONFIABLE por DISCREPANCIA
    Y el valor consolidado de TEMPERATURA_PRIMARIO es 300,00
    Y TEMPERATURA_PRIMARIO avisa "CANAL_EXCLUIDO:C"

  # CP-08 · Valor límite: exactamente 2,00 % no supera "más del 2 %"
  @CP-08
  Escenario: Una discrepancia de exactamente el 2 % no excluye al canal
    Dado que los canales A y B de TEMPERATURA_PRIMARIO informan 300,00
    Cuando el canal C de TEMPERATURA_PRIMARIO informa 306,60
    Entonces el canal C de TEMPERATURA_PRIMARIO queda CONFIABLE
    Y TEMPERATURA_PRIMARIO no tiene avisos
