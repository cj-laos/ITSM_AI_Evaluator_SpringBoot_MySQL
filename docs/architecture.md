# Arquitectura

## Fase 1: clasificación experimental

```text
XLSX/CSV
   |
   v
Importación y validación ---> MySQL: datasets + dataset_tickets
   |
   v
Partición estratificada (TRAIN / VALIDATION / TEST)
   |
   v
Orquestador de comparación
   +----> Spring AI / OpenAI
   +----> Spring AI / Gemini
   +----> Spring AI / Claude
                 |
                 v
     evaluation_results (una fila por ticket/modelo)
                 |
                 v
     métricas + matrices de confusión
```

Cada proveedor implementa `AiClassifier`. La lógica del experimento depende de esa
interfaz, no del SDK concreto. Esto permite sustituir un modelo o agregar un proveedor
sin alterar la importación, el esquema de resultados ni el cálculo estadístico.

La ejecución es asíncrona. Existe una ejecución (`evaluation_run`) por proveedor y todos
usan la misma partición. Dentro de cada ejecución las llamadas son secuenciales para que
la latencia no quede distorsionada por cientos de solicitudes simultáneas. El tamaño del
pool predeterminado es 3: una ejecución por proveedor.

## Fase 2: asignación adaptativa

La salida válida de fase 1 (`categoria_id`, `severidad_id`) será la entrada del futuro
servicio de asignación. Este consultará técnicos activos/disponibles, normalizará afinidad,
disponibilidad y carga, calculará el índice de idoneidad y registrará todos los candidatos.

El detalle de candidatos evita una “caja negra”: permite explicar por qué se seleccionó
un técnico y reproducir el cálculo con otra política de pesos.

## Decisiones de reproducibilidad

- Prompt versionado e inmutable por ejecución.
- Modelo guardado por `runId`.
- Partición persistida en cada ticket.
- Semilla explícita.
- Resultado bruto guardado.
- Fallos y respuestas inválidas conservados.
- Costos calculados desde precios configurables, no codificados en el servicio.
