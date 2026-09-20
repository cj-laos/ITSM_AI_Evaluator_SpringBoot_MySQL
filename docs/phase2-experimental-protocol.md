# Protocolo experimental de la fase técnica 2

## Objetivo

Evaluar el algoritmo multicriterio de asignación adaptativa utilizando como entrada las
clasificaciones de categoría y severidad generadas por GPT-5.6 Sol para los 300 tickets
del conjunto TEST.

## Alcance y limitación

La organización no conserva un registro estructurado de la disponibilidad, carga o
decisiones de asignación realizadas por llamada o WhatsApp. Por ello, la fase 2 se evalúa
como una simulación computacional controlada. Los resultados describen el comportamiento
del artefacto bajo condiciones reproducibles y no un despliegue histórico en producción.

## Configuración inicial

- Tres técnicos activos y disponibles.
- Carga inicial igual a cero.
- Los estados activo y disponible permanecen constantes durante los 300 tickets.
- Matriz de especialidades expresada en niveles de 0 a 4.
- Afinidad normalizada como `nivel / 4`.
- Carga ponderada por severidad: baja=1, media=2, alta=3 y crítica=4.
- Orden de procesamiento: `ticket_id` ascendente dentro de la ejecución seleccionada.
- Política inicial: `controlled-simulation-v2`.

La matriz incluida es provisional. Antes de la ejecución formal, el experto debe revisar
los quince niveles (tres técnicos por cinco categorías) y registrar su nombre mediante el
endpoint de actualización.

## Fórmula

```text
II(i,j) = alpha(s) * A(i,j) + gamma(s) * (1 - C(i))
```

`A(i,j)` es la afinidad del técnico con la categoría y `C(i)` es la carga acumulada
simulada, normalizada respecto de la mayor carga entre candidatos elegibles. Los pesos
de afinidad y baja carga suman 1.

Antes del cálculo se define el conjunto elegible:

```text
E(j) = {i | activo(i)=1, disponible(i)=1, A(i,j) >= umbral(severidad(j))}
```

Los umbrales iniciales son 0.25 para severidad baja, 0.50 para media y 0.75 para alta y
crítica. Activo representa la vigencia del técnico en la organización; disponible es una
condición temporal booleana. Ambos se utilizan como filtros y no como criterios ponderados.
Los registros inactivos se conservan para mantener la trazabilidad histórica, pero no
participan en nuevas asignaciones.

## Estrategias

1. `MULTICRITERIA`: artefacto propuesto.
2. `AFFINITY_ONLY`: línea base que selecciona por especialidad y usa carga como desempate.
3. `ROUND_ROBIN`: línea base que rota determinísticamente entre candidatos elegibles.

Las tres estrategias deben utilizar el mismo `evaluationRunId`, orden de tickets,
disponibilidad inicial y matriz de especialidades.

## Escenarios

| Código | Disponibilidad inicial | Carga inicial | Objetivo |
| --- | --- | --- | --- |
| E1 | Todos = 1 | Todos = 0 | Escenario principal homogéneo |
| E2 | Un técnico = 0 | Todos = 0 | Respuesta ante indisponibilidad |
| E3 | Todos = 1 | Cargas diferentes | Adaptación ante desequilibrio |

## Métricas

- tasa de asignaciones válidas;
- tasa de asignación respecto del total;
- afinidad media del técnico seleccionado;
- disponibilidad media como comprobación del filtro;
- índice de idoneidad medio;
- distribución de tickets por técnico;
- carga ponderada final por técnico;
- desviación estándar de la carga ponderada;
- rango de carga e índice de equidad de Jain;
- tiempo medio de cálculo por ticket;
- latencia p95;
- tasa de atención especializada de tickets altos y críticos;
- cantidad de tickets sin técnico elegible.

Las métricas se calculan automáticamente al finalizar cada simulación y se persisten en
`assignment_experiments`.

## Congelamiento experimental

Antes de ejecutar TEST se deben conservar: `evaluationRunId`, versión de política, matriz
de especialidades, disponibilidad inicial, cargas iniciales, estrategia y escenario. No
se deben ajustar los pesos después de observar los resultados de TEST bajo la misma
versión de política.

## Validación experta

Se recomienda seleccionar una muestra de decisiones estratificada por categoría y
severidad. El experto calificará cada asignación como válida, válida con observaciones o
no válida. Esta revisión complementa la simulación y no convierte la asignación manual
histórica en una única respuesta correcta.
