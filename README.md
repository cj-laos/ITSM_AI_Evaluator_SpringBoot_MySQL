# ITSM AI Evaluator

Backend independiente para ejecutar y comparar el triaje semántico de incidencias con
OpenAI, Gemini y Claude. Importa el dataset, crea una partición estratificada reproducible,
procesa el mismo conjunto `TEST` con cada proveedor y conserva predicciones, latencia,
tokens, caché, costo estimado y métricas.

## Decisión experimental

El dataset de 1000 tickets se divide una sola vez:

- `TRAIN`: 60 % (aproximadamente 600 tickets), reservado para fine-tuning futuro.
- `VALIDATION`: 10 % (aproximadamente 100), para calibrar el prompt y sus reglas.
- `TEST`: 30 % (aproximadamente 300), para la comparación final.

Los tres modelos reciben **los mismos tickets de TEST**. No se asignan 100 tickets distintos
a cada proveedor, porque eso impediría una comparación pareada y justa.

## Tecnologías

- Java 25 y Spring Boot 3.5.16.
- Spring AI 1.1.8.
- MySQL 8.4 y Flyway.
- Apache POI/Commons CSV para importar `.xlsx` y `.csv`.
- Actuator para salud y observabilidad.

## 1. Requisitos

- JDK 25.
- Docker Desktop con `docker compose`, o MySQL 8 instalado.
- Maven 3.9+, aunque en Linux/macOS también está incluido `./mvnw`.
- Una API key de cada proveedor que quieras habilitar.

No guardes claves reales en Git.

## 2. Base de datos

Desde la raíz:

```bash
cp .env.example .env
docker compose --env-file .env up -d
```

MySQL queda disponible en `localhost:3307`. Al iniciar el backend, Flyway crea
automáticamente todas las tablas con:

- `V1__classification_evaluation.sql`: dataset, ejecuciones, resultados y métricas.
- `V2__adaptive_assignment_foundation.sql`: estructura preparada para técnicos y
  asignación adaptativa.

No debes ejecutar manualmente esos SQL salvo que quieras inspeccionarlos.

## 3. Variables de entorno

Copia `.env.example` y completa las API keys. En Linux/macOS puedes cargarlo así:

```bash
set -a
source .env
set +a
```

En IntelliJ IDEA agrega las variables de `.env` en la configuración de ejecución.
En PowerShell puedes definirlas, por ejemplo:

```powershell
$env:OPENAI_ENABLED="true"
$env:OPENAI_API_KEY="tu_clave"
$env:GEMINI_ENABLED="true"
$env:GEMINI_API_KEY="tu_clave"
$env:CLAUDE_ENABLED="true"
$env:ANTHROPIC_API_KEY="tu_clave"
```

Los modelos están externalizados:

- `OPENAI_MODEL=gpt-5.6`
- `GEMINI_MODEL=gemini-3.1-pro-preview`
- `CLAUDE_MODEL=claude-opus-4-7`

Antes de la corrida formal confirma que tu cuenta tiene acceso a esos identificadores.
Si el proveedor cambia el nombre, modifica la variable, no el código. Se incluyen como
valores iniciales las tarifas estándar publicadas para prompts cortos: GPT-5.6
($5/$0.50 en caché/$30), Gemini 3.1 Pro Preview ($2/$0.20 en caché/$12) y Claude
Opus 4.7 ($5/$0.50 lectura de caché/$6.25 escritura de caché de 5 min/$25), siempre por
millón de tokens. Verifica nuevamente modelo, región, modalidad y precios el día de la prueba.

## 4. Ejecutar

```bash
./mvnw spring-boot:run
```

En Windows, con Maven instalado:

```powershell
mvn spring-boot:run
```

Comprueba:

```text
GET http://localhost:8080/actuator/health
GET http://localhost:8080/api/evaluations/providers
```

El segundo endpoint muestra solo proveedores habilitados y correctamente construidos.

Antes de gastar en la corrida completa, realiza una prueba de humo con un ticket:

```bash
curl -X POST http://localhost:8080/api/classifications \
  -H "Content-Type: application/json" \
  -d '{"provider":"OPENAI","description":"La impresora de logística no enciende; las demás funcionan."}'
```

Repite cambiando `provider` por `GEMINI` y `CLAUDE`. Esta ruta no guarda una ejecución
experimental; sirve únicamente para comprobar credenciales, modelo y formato JSON.

## 5. Importar el dataset

El archivo listo para usar está en:

`sample-data/Dataset_Corregido_Tesis_LOSMATUS.xlsx`

```bash
curl -X POST "http://localhost:8080/api/datasets/import?name=LOSMATUS&version=v1.0&description=Dataset%20validado" \
  -F "file=@sample-data/Dataset_Corregido_Tesis_LOSMATUS.xlsx"
```

La respuesta devuelve `datasetId`. El importador usa la hoja `Dataset_Modelo` y valida:

- `ticket_id` único;
- descripción no vacía;
- `categoria_id` entre 1 y 5;
- `severidad_id` entre 1 y 4.

También genera un SHA-256 de la descripción para detectar contaminación o duplicados en
análisis posteriores.

## 6. Crear la partición 60/10/30

Reemplaza `1` por el `datasetId` retornado:

```bash
curl -X POST http://localhost:8080/api/datasets/1/split \
  -H "Content-Type: application/json" \
  -d '{"trainRatio":0.6,"validationRatio":0.1,"testRatio":0.3,"seed":20260720}'
```

La separación es determinista y estratificada por la combinación
`categoria_id + severidad_id`. El ajuste final conserva exactamente 600/100/300 para
1000 registros. Conserva el `seed` en el protocolo de tesis.

## 7. Ejecutar los tres modelos

```bash
curl -X POST http://localhost:8080/api/evaluations/comparisons \
  -H "Content-Type: application/json" \
  -d '{"datasetId":1,"split":"TEST","providers":["OPENAI","GEMINI","CLAUDE"]}'
```

La respuesta entrega tres `runIds`. Cada ejecución procesa sus tickets secuencialmente;
las tres ejecuciones pueden avanzar en paralelo. Así se evita enviar 300 descripciones
en un solo prompt y cada observación conserva su propia latencia, tokens y respuesta.

Consulta el avance:

```text
GET /api/evaluations/{runId}
GET /api/evaluations/{runId}/results
GET /api/evaluations/{runId}/metrics
```

Si el proceso se interrumpe:

```text
POST /api/evaluations/{runId}/resume
```

El índice único `(run_id, ticket_id)` impide duplicar resultados.

## Preparar fine-tuning sin contaminar TEST

Después de crear la partición se pueden descargar:

```text
GET /api/datasets/{datasetId}/fine-tuning/canonical.jsonl?split=TRAIN
GET /api/datasets/{datasetId}/fine-tuning/openai.jsonl?split=TRAIN
GET /api/datasets/{datasetId}/fine-tuning/canonical.jsonl?split=VALIDATION
GET /api/datasets/{datasetId}/fine-tuning/openai.jsonl?split=VALIDATION
```

El formato canónico es neutral respecto del proveedor. El segundo genera el esquema de
mensajes para un flujo de fine-tuning de OpenAI. El backend bloquea expresamente la
exportación de `TEST` por estas rutas. Crear y administrar el job remoto de entrenamiento
se mantiene separado, porque su disponibilidad y formato no son equivalentes entre
OpenAI, Gemini y Claude; el identificador del modelo resultante se ingresa luego mediante
las variables `*_MODEL`.

## Métricas generadas

Para categoría y severidad:

- accuracy;
- precision, recall y F1 por clase;
- macro precision, macro recall y macro F1;
- weighted precision, weighted recall y weighted F1;
- matriz de confusión.

Además:

- coincidencia conjunta exacta de categoría y severidad;
- cobertura de respuestas válidas;
- latencia media, p50, p95 y p99;
- tokens de entrada/salida y tokens de caché cuando el proveedor los reporta;
- costo estimado por ticket y por ejecución.

Una respuesta inválida o un error de API cuenta como fallo en la exactitud de extremo a
extremo si continúa fallando después de los reintentos; no se elimina silenciosamente.
Los tokens, costo y latencia almacenados incluyen todos los intentos. La cobertura permite
distinguir calidad de clasificación y confiabilidad operativa.

## Prompt y caché

El prompt versionado está en
`src/main/resources/prompts/itsm-classifier-v1.1.txt`. Se envía como mensaje de sistema
idéntico en todas las solicitudes y únicamente cambia la descripción del usuario. Esa
estructura mantiene la comparación equivalente y permite aprovechar el cacheado de
prefijos cuando el proveedor/modelo lo soporta.

No cambies el prompt durante una ejecución formal. Si se modifica, crea `v1.2`, actualiza
`app.prompt.resource` y `app.prompt.version`, y genera nuevas ejecuciones. Nunca mezcles
versiones bajo el mismo `runId`.

## Segunda fase: asignación adaptativa implementada

La fase 2 consume una ejecución de clasificación ya almacenada en `evaluation_results`.
Para el artículo se debe utilizar el `runId` de GPT-5.6 Sol sobre los 300 tickets de
`TEST`. No vuelve a llamar al LLM: emplea directamente `predicted_category_id` y
`predicted_severity_id` como entrada del asignador.

Las migraciones `V3`, `V4` y `V5` incorporan:

- los técnicos Cristhoper Hilario, Gianfranco Souza y Lino Salcedo;
- una matriz provisional de niveles de especialidad por las cinco categorías;
- una política editable de pesos por severidad;
- experimentos de simulación vinculados con la ejecución de clasificación;
- trazabilidad de todos los candidatos, factores, índice y decisión final;
- comparación con las líneas base `AFFINITY_ONLY` y `ROUND_ROBIN`.
- disponibilidad booleana como restricción de elegibilidad;
- afinidad mínima configurable por severidad;
- métricas automáticas para el análisis del artículo.

La fórmula implementada es:

```text
II(i,j) = alpha(s) * afinidad(i,j)
        + gamma(s) * (1 - cargaNormalizada(j))
```

Antes del cálculo se filtran los técnicos que no estén activos, no estén disponibles o
no alcancen la afinidad mínima de la severidad. Disponibilidad no recibe un peso: es una
condición booleana obligatoria.

La carga se acumula secuencialmente en unidades ponderadas por severidad (1, 2, 3 y 4)
y se normaliza respecto de la mayor carga de los candidatos elegibles en cada decisión.
Si todos comienzan con carga cero y disponibilidad 1, la primera selección depende de
la afinidad; las decisiones siguientes incorporan la carga acumulada.

### Configuración antes de la prueba formal

Consulta la matriz y los pesos:

```text
GET /api/assignments/configuration/technicians
GET /api/assignments/configuration/policies/controlled-simulation-v2
GET /api/assignments/configuration/policies/controlled-simulation-v2/thresholds
```

Los niveles provisionales usan la escala 0–4 y deben ser revisados por el responsable
técnico antes de congelar el experimento. Pueden modificarse mediante:

```text
PUT /api/assignments/configuration/technicians/{technicianId}/skills/{categoryId}
```

Los pesos también son parametrizables, pero afinidad y baja carga deben
sumar exactamente 1:

```text
PUT /api/assignments/configuration/policies/{policyVersion}/severities/{severityId}
PUT /api/assignments/configuration/policies/{policyVersion}/thresholds/{severityId}
```

Una vez validada la configuración, no se debe modificar la misma versión de política.
Para una calibración diferente, crea una nueva versión y conserva ambas para garantizar
la reproducibilidad.

### Simulación principal sobre los 300 tickets

Reemplaza `5` por el `runId` real de GPT-5.6 Sol:

```bash
curl -X POST http://localhost:8080/api/assignments/simulations \
  -H "Content-Type: application/json" \
  -d '{
    "evaluationRunId": 5,
    "policyVersion": "controlled-simulation-v2",
    "strategy": "MULTICRITERIA",
    "scenario": "E1_CARGA_CERO_TODOS_DISPONIBLES",
    "availabilityOverrides":{"TEC-001":true,"TEC-002":true,"TEC-003":true},
    "initialLoads":{"TEC-001":0,"TEC-002":0,"TEC-003":0}
  }'
```

Todas las métricas se calculan automáticamente, se guardan en `assignment_experiments`
y se devuelven en el JSON: tasa de asignación y validez, afinidad e idoneidad medias,
tiempo medio y p95, desviación y rango de carga, índice de Jain, atención especializada
de tickets altos/críticos y distribución final. La trazabilidad por ticket se consulta mediante:

```text
GET /api/assignments/experiments/{experimentId}
GET /api/assignments/experiments/{experimentId}/trace
```

Ejecuta el mismo `runId` con `AFFINITY_ONLY` y `ROUND_ROBIN` para obtener las líneas base.
Los ejemplos completos se encuentran en `requests.http`.

### Alcance experimental

La prueba es una simulación computacional controlada porque la organización no dispone
de registros estructurados de disponibilidad, carga o asignaciones realizadas por llamada
y WhatsApp. El escenario principal inicia con los tres técnicos disponibles y sin carga.
Activo y disponible permanecen constantes durante los 300 tickets; solamente se actualiza
la carga acumulada simulada. No se implementan límites automáticos de incidencias ni
eventos de resolución porque no existen tiempos operativos históricos para modelarlos.
Los resultados no deben presentarse como evidencia histórica de producción. La matriz de
especialidades y una muestra de decisiones deben ser validadas por el experto.

### Pruebas automatizadas

`AdaptiveAssignmentEngineTest` verifica:

- selección por mayor afinidad cuando las condiciones iniciales son iguales;
- adaptación ante carga acumulada;
- exclusión por indisponibilidad o falta de especialidad;
- exclusión por no alcanzar la afinidad mínima de la severidad;
- round-robin determinista;
- validación de que los pesos sumen 1.

Ejecuta las pruebas con JDK 25 y acceso a las dependencias Maven:

```bash
./mvnw test
```

## Controles para la tesis

- Congela dataset, `seed`, prompt, modelos y precios antes de TEST.
- Usa VALIDATION para corregir reglas; no observes TEST para volver a ajustar el prompt.
- Si haces fine-tuning, usa solo TRAIN y VALIDATION. TEST permanece intacto.
- Ejecuta los proveedores bajo condiciones comparables y registra fecha/hora.
- Reporta métricas macro por el posible desbalance entre clases.
- No presentes la asignación histórica del técnico como única verdad correcta.

Ejemplos listos para IntelliJ/VS Code: [`requests.http`](requests.http).
